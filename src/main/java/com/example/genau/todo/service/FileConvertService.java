package com.example.genau.todo.service;

import com.example.genau.todo.dto.TodolistCreateRequest;
import com.example.genau.todo.dto.TodolistUpdateRequest; // ✅ 추가
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.repository.TodolistRepository;
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

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TodolistRepository todolistRepository;

    public FileConvertService(TodolistRepository todolistRepository) {
        this.todolistRepository = todolistRepository;
    }

    public Resource convertFile(MultipartFile file, String targetFormat, Long todoId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + todoId));

        try {
            todo.setConvertStatus("WAITING");
            todolistRepository.save(todo);
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

            Request jobRequest = new Request.Builder()
                    .url(API_URL + "/jobs")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(createJobJson, MediaType.parse("application/json")))
                    .build();

            Response jobResponse = httpClient.newCall(jobRequest).execute();
            JsonNode jobJson = objectMapper.readTree(Objects.requireNonNull(jobResponse.body()).string());
            String uploadUrl = jobJson.at("/data/tasks").get(0).get("result").get("form").get("url").asText();
            String jobId = jobJson.get("data").get("id").asText();
            String taskId = jobJson.at("/data/tasks").get(0).get("id").asText();

            // Step 2: Upload file to CloudConvert
            JsonNode form = jobJson.at("/data/tasks").get(0).get("result").get("form");
            uploadUrl = form.get("url").asText();
            JsonNode parameters = form.get("parameters");

            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

// ⚠️ form parameters를 모두 추가
            parameters.fields().forEachRemaining(entry -> {
                bodyBuilder.addFormDataPart(entry.getKey(), entry.getValue().asText());
            });

// ⚠️ 실제 파일도 함께 전송
            bodyBuilder.addFormDataPart(
                    "file",
                    file.getOriginalFilename(),
                    RequestBody.create(file.getBytes(), MediaType.parse("application/octet-stream"))
            );


            Request uploadRequest = new Request.Builder()
                    .url(uploadUrl)
                    .post(bodyBuilder.build())
                    .build();

            httpClient.newCall(uploadRequest).execute().close();

            // Step 3: Poll job status and get result URL
            String exportUrl = pollForExportUrl(jobId);

            // Step 4: Download result file
            Request downloadRequest = new Request.Builder().url(exportUrl).build();
            Response fileResponse = httpClient.newCall(downloadRequest).execute();
            byte[] fileBytes = Objects.requireNonNull(fileResponse.body()).bytes();


            // Step 5: Save to Local Disk
            String convertedFileName = todoId + "_converted." + targetFormat;
            Path savePath = Paths.get(System.getProperty("user.dir"), "converted", convertedFileName);
            Files.createDirectories(savePath.getParent());
            Files.write(savePath, fileBytes);

            // Step 6: Update DB
            todo.setConvertedFileUrl(savePath.toString());
            todo.setConvertStatus("SUCCESS"); // ✅
            todo.setConvertedAt(java.time.LocalDateTime.now()); // ✅
            todolistRepository.save(todo);


            return new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return convertedFileName;
                }

                @Override
                public long contentLength() {
                    return fileBytes.length;
                }
            };

        } catch (IOException e) {
            todo.setConvertStatus("FAILED");
            todolistRepository.save(todo);
            throw new RuntimeException("파일 변환 중 오류가 발생했습니다: " + e.getMessage(), e);
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

}

