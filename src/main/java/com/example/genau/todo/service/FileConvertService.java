package com.example.genau.todo.service;

import com.example.genau.team.domain.Team;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
import com.example.genau.todo.dto.TodolistCreateRequest;
import com.example.genau.todo.dto.TodolistUpdateRequest; // ✅ 추가
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.repository.TodolistFileRepository;
import com.example.genau.todo.repository.TodolistRepository;
import com.example.genau.todo.entity.TodolistFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import java.io.*;
import java.nio.file.Files;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;


@Service
public class FileConvertService {

    @Value("${cloudconvert.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.cloudconvert.com/v2";

    private final TodolistFileRepository todolistFileRepository;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TodolistRepository todolistRepository;
    private final TeamRepository teamRepository;
    private final TeammatesRepository teammatesRepository;
    public FileConvertService(TodolistRepository todolistRepository, TeamRepository teamRepository, TeammatesRepository teammatesRepository,  TodolistFileRepository todolistFileRepository) {

        this.todolistRepository = todolistRepository;
        this.teamRepository = teamRepository;
        this.teammatesRepository = teammatesRepository;
        this.todolistFileRepository = todolistFileRepository;
    }

    public Resource convertFile(MultipartFile file, String targetFormat, Long fileId, Long userId) {
        System.out.println("🔄 convertFile 호출됨");

        TodolistFile todolistFile = todolistFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        Todolist todo = todolistFile.getTodolist();

        // 권한 체크
        validateTeamMembership(todo.getTeamId(), userId);

        try {
            todolistFile.setConvertStatus("WAITING");
            todolistFileRepository.save(todolistFile);

            // ✅ 저장된 파일 경로에서 파일 읽기
            Path savedFilePath = Paths.get(todolistFile.getFilePath());
            if (!Files.exists(savedFilePath)) {
                throw new IOException("저장된 파일을 찾을 수 없습니다: " + savedFilePath);
            }

            byte[] fileBytes = Files.readAllBytes(savedFilePath);
            String originalFileName = todolistFile.getFileName();
            System.out.println("✅ 파일 읽기 완료: " + originalFileName + " (" + fileBytes.length + " bytes)");

            // Step 1: Create Job
            String createJobJson = "{\n" +
                    "  \"tasks\": {\n" +
                    "    \"upload-my-file\": {\n" +
                    "      \"operation\": \"import/upload\"\n" +
                    "    },\n" +
                    "    \"convert-my-file\": {\n" +
                    "      \"operation\": \"convert\",\n" +
                    "      \"input\": \"upload-my-file\",\n" +
                    "      \"output_format\": \"" + targetFormat + "\"\n" +
                    "    },\n" +
                    "    \"export-my-file\": {\n" +
                    "      \"operation\": \"export/url\",\n" +
                    "      \"input\": \"convert-my-file\",\n" +
                    "      \"inline\": true,\n" +
                    "      \"archive_multiple_files\": false\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            System.out.println("📤 CloudConvert Job 생성 중...");

            Request jobRequest = new Request.Builder()
                    .url(API_URL + "/jobs")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(createJobJson, MediaType.parse("application/json")))
                    .build();

            Response jobResponse = httpClient.newCall(jobRequest).execute();

            System.out.println("📥 CloudConvert 응답 코드: " + jobResponse.code());

            if (!jobResponse.isSuccessful()) {
                String errorBody = jobResponse.body() != null ? jobResponse.body().string() : "No error body";
                System.err.println("❌ CloudConvert API 오류: " + errorBody);
                throw new RuntimeException("CloudConvert API 오류: " + jobResponse.code() + " - " + errorBody);
            }

            String responseBodyStr = Objects.requireNonNull(jobResponse.body()).string();
            JsonNode jobJson = objectMapper.readTree(responseBodyStr);
            JsonNode tasks = jobJson.at("/data/tasks");

            if (!tasks.isArray() || tasks.isEmpty()) {
                throw new RuntimeException("CloudConvert job 생성 실패: tasks 없음");
            }

            JsonNode formNode = tasks.get(0).path("result").path("form");
            if (formNode.isMissingNode() || formNode.path("url").isMissingNode()) {
                throw new RuntimeException("CloudConvert 응답에 업로드 form 없음");
            }

            String uploadUrl = formNode.get("url").asText();
            JsonNode parameters = formNode.get("parameters");

            System.out.println("📤 파일 업로드 중: " + uploadUrl);

            // Step 2: Upload file
            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

            parameters.fields().forEachRemaining(entry -> {
                bodyBuilder.addFormDataPart(entry.getKey(), entry.getValue().asText());
            });

            bodyBuilder.addFormDataPart(
                    "file",
                    originalFileName,
                    RequestBody.create(fileBytes, MediaType.parse("application/octet-stream"))
            );

            Request uploadRequest = new Request.Builder()
                    .url(uploadUrl)
                    .post(bodyBuilder.build())
                    .build();

            httpClient.newCall(uploadRequest).execute().close();
            System.out.println("✅ 파일 업로드 완료");

            // Step 3: Poll job status
            String jobId = jobJson.get("data").get("id").asText();
            System.out.println("🔄 변환 상태 확인 중... Job ID: " + jobId);
            String exportUrl = pollForExportUrl(jobId);

            // Step 4: Download result
            System.out.println("📥 변환된 파일 다운로드 중...");
            Request downloadRequest = new Request.Builder().url(exportUrl).build();
            Response fileResponse = httpClient.newCall(downloadRequest).execute();
            byte[] convertedBytes = Objects.requireNonNull(fileResponse.body()).bytes();

            // ✅ Step 5: 원본 파일명에서 확장자만 변경
            String baseFileName = originalFileName;
            int lastDotIndex = originalFileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                baseFileName = originalFileName.substring(0, lastDotIndex);
            }
            String convertedFileName = baseFileName + "." + targetFormat;

            // ✅ storage/team-X/ 경로에 저장
            Long teamId = todo.getTeamId();
            String storageDir = System.getProperty("user.dir") + "/storage/team-" + teamId;
            Path storagePath = Paths.get(storageDir);
            Files.createDirectories(storagePath);

            Path convertedFilePath = storagePath.resolve(convertedFileName);
            Files.write(convertedFilePath, convertedBytes);

            System.out.println("✅ 변환 파일 저장 완료: " + convertedFilePath);

            // Step 6: 개별 파일 정보 업데이트
            todolistFile.setConvertedFilePath(convertedFilePath.toString());
            todolistFile.setConvertStatus("SUCCESS");
            todolistFile.setConvertedAt(java.time.LocalDateTime.now());
            todolistFileRepository.save(todolistFile);

            // Todo 전체 변환 상태 업데이트
            updateTodoConvertStatus(todo);

            System.out.println("✅ 변환 완료: " + originalFileName + " -> " + convertedFileName);

            return new ByteArrayResource(convertedBytes) {
                @Override
                public String getFilename() {
                    return convertedFileName;
                }

                @Override
                public long contentLength() {
                    return convertedBytes.length;
                }
            };

        } catch (IOException e) {
            System.err.println("❌ 파일 변환 실패: " + e.getMessage());
            e.printStackTrace();

            todolistFile.setConvertStatus("FAILED");
            todolistFileRepository.save(todolistFile);

            updateTodoConvertStatus(todo);

            throw new RuntimeException("파일 변환 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // ✅ 새 메서드: Todo의 전체 변환 상태 계산
    private void updateTodoConvertStatus(Todolist todo) {
        List<TodolistFile> files = todo.getFiles();

        if (files.isEmpty()) {
            todo.setConvertStatus(null);
            todolistRepository.save(todo);
            return;
        }

        boolean hasWaiting = files.stream().anyMatch(f -> "WAITING".equals(f.getConvertStatus()));
        boolean hasFailed = files.stream().anyMatch(f -> "FAILED".equals(f.getConvertStatus()));
        boolean allSuccess = files.stream().allMatch(f -> "SUCCESS".equals(f.getConvertStatus()));

        if (hasWaiting) {
            todo.setConvertStatus("WAITING");
        } else if (hasFailed) {
            todo.setConvertStatus("FAILED");
        } else if (allSuccess) {
            todo.setConvertStatus("SUCCESS");
        }

        todolistRepository.save(todo);
    }

    // ✅ convertToPdf 메서드 수정
    public void convertToPdf(TodolistFile fileEntity, MultipartFile file) {
        Long fileId = fileEntity.getId();
        Long userId = fileEntity.getUploader().getUserId();

        convertFile(file, "pdf", fileId, userId);
    }
    // ✅ 팀원인지 확인하는 공통 메서드 추가
    private void validateTeamMembership(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("해당 팀이 없습니다."));

        boolean isMember = team.getUserId().equals(userId)
                || teammatesRepository.existsByTeamIdAndUserId(teamId, userId);

        if (!isMember) {
            throw new AccessDeniedException("팀원만 파일 변환을 할 수 있습니다.");
        }
    }


    private String pollForExportUrl(String jobId) throws IOException {
        int retries = 90;
        while (retries-- > 0) {
            System.out.println("🔄 Checking job status... (remaining retries: " + retries + ")");
            Request checkRequest = new Request.Builder()
                    .url(API_URL + "/jobs/" + jobId)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            Response response = httpClient.newCall(checkRequest).execute();
            String jsonString = Objects.requireNonNull(response.body()).string();
            JsonNode root = objectMapper.readTree(jsonString);
            JsonNode tasks = root.at("/data/tasks");

            for (JsonNode task : tasks) {
                String name = task.get("name").asText();
                String status = task.get("status").asText();
                System.out.println("Task name: " + name + ", Status: " + status);

                if ("export-my-file".equals(name) && "finished".equals(status)) {
                    String url = task.get("result").get("files").get(0).get("url").asText();
                    System.out.println("✅ Export file ready at: " + url);
                    return url;
                } else if ("convert-my-file".equals(name) && "error".equals(status)) {
                    System.out.println("❌ Conversion error: " + task.toString());
                    throw new RuntimeException("CloudConvert 변환 실패: " + task.toString());
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }
        throw new RuntimeException("파일 변환이 시간 내에 완료되지 않았습니다.");
    }

    public void convertToFormat(TodolistFile fileEntity, MultipartFile file, String targetFormat) {
        System.out.println("🔄 변환 시작: " + fileEntity.getFileName() + " -> " + targetFormat);

        if (fileEntity.getId() == null) {
            System.err.println("❌ TodolistFile ID가 null입니다.");
            return;
        }

        Long fileId = fileEntity.getId();
        Long userId = fileEntity.getUploader().getUserId();

        try {
            // ✅ 이제 file 파라미터는 사용하지 않음 (디스크에서 읽음)
            convertFile(null, targetFormat, fileId, userId);  // file을 null로 전달
            System.out.println("✅ 변환 완료: " + fileEntity.getFileName());
        } catch (Exception e) {
            System.err.println("❌ 파일 변환 실패: " + fileEntity.getFileName());
            e.printStackTrace();
            fileEntity.setConvertStatus("FAILED");
            todolistFileRepository.save(fileEntity);
        }
    }

}

