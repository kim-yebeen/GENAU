package com.example.genau.todo.service;

import com.example.genau.category.domain.Category;
import com.example.genau.category.repository.CategoryRepository;
import com.example.genau.todo.handler.TodoUpdateHandler;
import com.example.genau.storage.service.StorageService;
import com.example.genau.team.domain.Teammates;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
import com.example.genau.todo.dto.*;
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.repository.TodolistRepository;
import com.example.genau.user.domain.User;
import com.example.genau.user.repository.UserRepository;
import com.example.genau.todo.entity.TodolistFile;
import com.example.genau.todo.util.FileValidationUtil;



import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.springframework.security.access.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.lang.Exception;
import com.example.genau.team.domain.Team;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.example.genau.notice.service.NotificationService;
import com.example.genau.user.domain.User;
import java.util.ArrayList;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class TodolistService {

    private final TodolistRepository todolistRepository;
    private final CategoryRepository categoryRepository;
    private final TeammatesRepository teammatesRepository;
    private final TeamRepository teamRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final TodoUpdateHandler todoUpdateHandler;
    private final UserRepository userRepository;
    private final FileConvertService fileConvertService;
    private final FileStorageService fileStorageService;

    private static final List<String> allowedExtensions = List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "csv", "jpg", "jpeg", "png", "gif"
    );



    private boolean isVisibleByDeadline(LocalDate dueDate) {
        LocalDate today = LocalDate.now();
        return dueDate != null && (
                !dueDate.isBefore(today.minusDays(3))
        );
    }

    public List<Todolist> getTodosByTeamId(Long teamId, Long userId) {
        validateTeamMembership(teamId, userId);
        return todolistRepository.findAllByTeamId(teamId);
    }

    private void validateTeamMembership(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("해당 팀이 없습니다."));

        boolean isMember = team.getUserId().equals(userId)
                || teammatesRepository.existsByTeamIdAndUserId(teamId, userId);

        if (!isMember) {
            throw new AccessDeniedException("팀원만 접근할 수 있습니다.");
        }
    }

    public Todolist createTodolist(TodolistCreateRequest request, Long userId, List<MultipartFile> files) {
        validateTeamMembership(request.getTeamId(), userId);

        Todolist todo = new Todolist();
        todo.setTeamId(request.getTeamId());
        todo.setCatId(request.getCatId());
        todo.setTodoTitle(request.getTodoTitle());
        todo.setTodoDes(request.getTodoDes());
        todo.setDueDate(request.getDueDate());
        todo.setTodoTime(LocalDateTime.now());
        //todo.setFileForm(request.getFileForm());
        todo.setTodoChecked(false);
        // ✅ creatorId → creator 객체로 설정
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));
        todo.setCreator(creator);

        // ✅ assigneeIds → assignees 객체로 설정
        if (request.getAssigneeIds() != null && !request.getAssigneeIds().isEmpty()) {
            List<User> users = request.getAssigneeIds().stream()
                    .map(id -> userRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id)))
                    .collect(Collectors.toList());
            todo.setAssignees(users);
        }

        // fileForm 설정 (nullable 허용)
        if (request.getFileForm() != null && !request.getFileForm().isBlank()) {
            todo.setFileForm(request.getFileForm());
        }

        /*String fileForm = request.getFileForm();
        if (fileForm != null && !fileForm.trim().isEmpty()) {
            validateFileExtension(fileForm); // 파일 확장자 유효성 검사
            todo.setFileForm(fileForm.trim().toLowerCase()); // 소문자로 저장 (일관성 유지)
        } else {
            todo.setFileForm(null); // 명시적 null 저장 (안 해도 되긴 함)
        }*/
        // ✅ 파일 업로드 처리
        List<TodolistFile> savedFiles = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String ext = FileValidationUtil.getExtension(file.getOriginalFilename());
                    if (todo.getFileForm() != null && !todo.getFileForm().equalsIgnoreCase(ext)) {
                        throw new IllegalArgumentException("파일 확장자가 요구 형식과 일치하지 않습니다: " + ext);
                    }

                    // 파일 저장
                    String uploadPath = fileStorageService.saveFile(file);
                    TodolistFile todolistFile = TodolistFile.builder()
                            .fileName(file.getOriginalFilename())
                            .filePath(uploadPath)
                            .contentType(file.getContentType())
                            .uploadedAt(LocalDateTime.now())
                            .todolist(todo)
                            .uploader(creator)
                            .build();

                    savedFiles.add(todolistFile);

                    // ✅ 변환 처리
                    fileConvertService.convertToPdf(file, uploadPath, todolistFile);
                }
            }
            todo.setFiles(savedFiles);
        }


        return todolistRepository.save(todo);
    }


    // TodolistService 클래스 안에 추가
    private void validateFileExtension(String fileForm) {
        List<String> allowedExtensions = List.of(
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "txt", "md", "csv", "jpg", "jpeg", "png", "gif"
        );

        if (!allowedExtensions.contains(fileForm.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않은 파일 확장자입니다: " + fileForm);
        }
    }


    public Todolist updateTodolist(Long todoId, TodolistUpdateRequest request, Long userId) {

        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        // 🔒 권한 체크: 생성자 또는 assignees 중 한 명이면 허용
        boolean isCreator = todo.getCreator() != null && todo.getCreator().getUserId().equals(userId);
        boolean isAssignee = todo.getAssignees() != null &&
                todo.getAssignees().stream().anyMatch(user -> user.getUserId().equals(userId));

        if (!isCreator && !isAssignee) {
            throw new AccessDeniedException("TODO 생성자 또는 담당자만 수정할 수 있습니다.");
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = todo.getDueDate();

        if (dueDate != null && today.isAfter(dueDate.plusDays(3))) {
            throw new IllegalStateException("마감일이 지난 지 3일이 넘어 수정할 수 없습니다.");
        }

        String oldTitle = todo.getTodoTitle();
        LocalDate oldDueDate = todo.getDueDate();

        if (dueDate != null && today.isAfter(dueDate)) {
            if (request.getFileForm() != null) todo.setFileForm(request.getFileForm());
        } else {
            if (request.getTodoTitle() != null) todo.setTodoTitle(request.getTodoTitle());
            if (request.getTodoDes() != null) todo.setTodoDes(request.getTodoDes());
            if (request.getDueDate() != null) todo.setDueDate(request.getDueDate());
            if (request.getFileForm() != null) todo.setFileForm(request.getFileForm());
            if (request.getAssigneeIds() != null) { // DTO가 List<Long> getAssigneeIds()를 반환한다고 가정
                if (request.getAssigneeIds().isEmpty()) {
                    todo.setAssignees(new ArrayList<>()); // 빈 리스트로 설정
                } else {
                    List<User> users = request.getAssigneeIds().stream()
                            .map(id -> userRepository.findById(id)
                                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + id)))
                            .collect(Collectors.toList());
                    todo.setAssignees(users);
                }
            }
        }

        todo.setTodoTime(LocalDateTime.now());
        Todolist savedTodo = todolistRepository.save(todo);

        // ✅ 웹소켓 브로드캐스트 (제목과 날짜 모두 포함)
        boolean titleChanged = !Objects.equals(oldTitle, savedTodo.getTodoTitle());
        boolean dateChanged = !Objects.equals(oldDueDate, savedTodo.getDueDate());

        if (titleChanged || dateChanged) {
            String message = String.format(
                    "{\"type\":\"TODO_UPDATED\", \"todoId\":%d, \"newTitle\":\"%s\", \"newDueDate\":\"%s\"}",
                    savedTodo.getTodoId(),
                    savedTodo.getTodoTitle(),
                    savedTodo.getDueDate() != null ? savedTodo.getDueDate().toString() : null
            );
            todoUpdateHandler.broadcast(message);

            System.out.println("📤 TODO 업데이트 브로드캐스트: " + message);
        }

        return savedTodo;
    }


    public void deleteTodolist(Long todoId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (todo.getCreator() == null || !todo.getCreator().getUserId().equals(userId)) {
            throw new AccessDeniedException("TODO 생성자만 삭제할 수 있습니다.");
        }
        todolistRepository.deleteById(todoId);
    }

    public Todolist updateTodolistWithFile(Long todoId, TodolistUpdateRequest request, Long userId, List<MultipartFile> files) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        List<String> allowedExtensionsList = List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "jpg", "jpeg", "png", "gif");
        String[] allowedExtensions = allowedExtensionsList.toArray(new String[0]);  // ✅ 변환


        // 권한 체크: 생성자이거나 담당자 중 하나일 경우에만 허용
        boolean isCreator = todo.getCreator() != null && todo.getCreator().getUserId().equals(userId);
        boolean isAssignee = todo.getAssignees() != null &&
                todo.getAssignees().stream().anyMatch(user -> user.getUserId().equals(userId));

        if (!isCreator && !isAssignee) {
            throw new AccessDeniedException("TODO 생성자 또는 담당자만 수정할 수 있습니다.");
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = todo.getDueDate();

        if (files != null && !files.isEmpty()) {
            if (dueDate != null && today.isAfter(dueDate)) {
                throw new IllegalStateException("마감일이 지난 후에는 파일을 변경할 수 없습니다.");
            }

            String oldFilePath = todo.getUploadedFilePath();

            try {

                // 기존 파일들 삭제
                List<TodolistFile> oldFiles = todo.getFiles();
                for (TodolistFile oldFile : oldFiles) {
                    //String oldFilePath = oldFile.getFilePath();
                    if (oldFilePath != null && !oldFilePath.isBlank()) {
                        // uploads 폴더 삭제
                        java.nio.file.Path oldPath = java.nio.file.Paths.get(oldFilePath);
                        java.nio.file.Files.deleteIfExists(oldPath);
                        System.out.println("기존 uploads 파일 삭제: " + oldPath);

                        // 스토리지 파일 삭제
                        storageService.deleteOldTodoFiles(todoId, oldFilePath);
                    }
                }

                // DB에서 연관관계 제거
                todo.getFiles().clear();

                // 새 파일 저장
                List<TodolistFile> newFiles = new ArrayList<>();
                String uploadDir = System.getProperty("user.dir") + "/uploads";
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                java.nio.file.Files.createDirectories(uploadPath);

                for (MultipartFile file : files) {
                    if (!FileValidationUtil.isValidExtension(file.getOriginalFilename(), allowedExtensions)) {
                        throw new IllegalArgumentException("허용되지 않은 확장자: " + file.getOriginalFilename());
                    }

                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    java.nio.file.Path filePath = uploadPath.resolve(fileName);
                    file.transferTo(filePath.toFile());

                    TodolistFile fileEntity = TodolistFile.builder()
                            .fileName(file.getOriginalFilename())
                            .filePath(filePath.toString())
                            .uploadedAt(LocalDateTime.now())
                            .uploader(userRepository.findById(userId).orElseThrow())
                            .todolist(todo)
                            .build();

                    newFiles.add(fileEntity);

                    // 스토리지 복사
                    storageService.copyToStorageImmediately(todoId, filePath.toString());
                }

                // 파일 목록 저장
                todo.getFiles().addAll(newFiles);

                // todo 정보 갱신
                todo.setTodoTime(LocalDateTime.now());
                todo.setTodoChecked(true); // 파일이 있으므로 체크

            } catch (Exception e) {
                throw new RuntimeException("파일 수정 실패: " + e.getMessage());
            }
        }

        // 다른 필드 업데이트
        if (request != null && hasNonNullFields(request)) {
            return updateTodolist(todoId, request, userId); // 기존 메서드 재사용
        }

        return todolistRepository.save(todo);
    }

    // ✅ request에 null이 아닌 필드가 있는지 확인하는 헬퍼 메서드

    private boolean hasNonNullFields(TodolistUpdateRequest request) {
        return request.getTodoTitle() != null ||
                request.getTodoDes() != null ||
                request.getDueDate() != null ||
                request.getFileForm() != null ||
                request.getAssigneeIds() != null;
    }


    // 체크 상태(완료 여부) 업데이트

    public void updateTodoChecked(Long todoId, boolean checked) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("해당 투두를 찾을 수 없습니다. todoId = " + todoId));
        todo.setTodoChecked(checked);
        todolistRepository.save(todo);
    }

    public boolean validateFileExtension(Long todoId, MultipartFile file) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        String requiredExtension = todo.getFileForm();
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 이름에 확장자가 없습니다.");
        }

        String submittedExtension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);

        return requiredExtension.equalsIgnoreCase(submittedExtension);
    }

    public String verifyFile(Long todoId, MultipartFile file) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        String requiredExtension = todo.getFileForm();

        if (requiredExtension == null || requiredExtension.isBlank()) {
            throw new IllegalArgumentException("요구하는 파일 확장자가 지정되지 않았습니다.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("업로드된 파일 이름에 확장자가 없습니다.");
        }

        String submittedExtension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);

        if (requiredExtension.equalsIgnoreCase(submittedExtension)) {
            return "파일 검증 성공: 요구한 확장자와 일치합니다.";
        } else {
            return "파일 검증 실패: 요구한 확장자(" + requiredExtension + ")와 제출된 파일 확장자(" + submittedExtension + ")가 다릅니다.";
        }
    }

    // ✅ 변경점 요약
// 1) 파라미터가 MultipartFile → List<MultipartFile> files 로 바뀜
// 2) 단일 파일 기준 로직을 for (MultipartFile file : files) 루프로 변경
// 3) 각 파일마다 확장자 검증, fileForm 매칭, 용량 제한, 저장, TodolistFile 추가
// 4) 마지막에 uploadedFilePath 하나만 세팅(하위 호환용), files 목록은 전부 유지

    public String submitFile(Long todoId, Long userId, List<MultipartFile> files) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        // ✅ 권한 체크: 담당자가 있으면 담당자만, 없으면 팀원 누구나 (기존 로직 유지)
        if (todo.getAssignees() != null && !todo.getAssignees().isEmpty()) {
            // 담당자가 지정된 경우: 담당자만 제출 가능
            boolean isAssignee = todo.getAssignees().stream()
                    .anyMatch(user -> user.getUserId().equals(userId));

            if (!isAssignee) {
                throw new AccessDeniedException("파일은 담당자만 제출할 수 있습니다.");
            }
        } else {
            // 담당자가 지정되지 않은 경우: 팀원이면 누구나 제출 가능
            validateTeamMembership(todo.getTeamId(), userId);
        }

        // ✅ 마감일 체크: 마감일 + 3일 이후에는 제출 불가 (기존 로직 유지)
        LocalDate today = LocalDate.now();
        LocalDate dueDate = todo.getDueDate();

        if (dueDate != null && today.isAfter(dueDate.plusDays(3))) {
            throw new IllegalStateException("마감일이 지난 지 3일이 넘어 업로드할 수 없습니다.");
        }

        // ✅ [변경] 단일 file null/empty 체크 → 리스트 전체 체크
        // 기존:
        // if (file.isEmpty()) { ... }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("제출할 파일이 없습니다.");
        }

        // ✅ 요구 확장자 (예: "pdf") — 이 값과 다른 확장자는 제출 불가
        String requiredExtension = todo.getFileForm(); // null 이면 형식 제한 없음

        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);

            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            // ✅ 업로더는 한 번만 조회
            User uploader = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // ✅ 여러 파일 경로를 저장하기 위한 리스트 (마지막 하나는 uploadedFilePath에 넣어줌)
            List<String> savedPaths = new ArrayList<>();

            // ✅ [핵심 변경] 단일 파일 처리 → 여러 파일 반복 처리
            // 기존에는:
            // String originalFilename = file.getOriginalFilename();
            // ...
            // file.transferTo(filePath.toFile());
            //
            // 이런 식으로 한 번만 처리하던 것을, 아래처럼 for 루프로 감쌈
            for (MultipartFile file : files) {

                // --- 파일 단위 기본 검증 ---
                if (file.isEmpty()) {
                    throw new IllegalArgumentException("비어 있는 파일이 포함되어 있습니다.");
                }

                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || !originalFilename.contains(".")) {
                    throw new IllegalArgumentException("파일 이름에 확장자가 없습니다: " + originalFilename);
                }

                String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

                // ✅ 허용 확장자 검증 (클래스 상단의 allowedExtensions 사용)
                if (!allowedExtensions.contains(extension)) {
                    throw new IllegalArgumentException("허용되지 않은 파일 확장자입니다: " + extension);
                }

                // ✅ fileForm과 제출 파일 확장자 강제 매칭 (기존 단일 파일 로직을 그대로 확장)
                if (requiredExtension != null && !requiredExtension.isBlank()) {
                    // fileForm은 "pdf", "docx" 와 같은 단일 확장자라고 가정
                    if (!requiredExtension.equalsIgnoreCase(extension)) {
                        throw new IllegalArgumentException(
                                "요구된 파일 형식(" + requiredExtension + ")과 다른 파일(" + originalFilename + ")은 제출할 수 없습니다."
                        );
                    }
                }

                // ✅ 용량 제한 (기존 로직 그대로 사용)
                long fileSizeInMB = file.getSize() / (1024 * 1024);
                boolean isMedia = List.of("mp3", "wav", "mp4", "avi", "mov").contains(extension);

                if (isMedia && fileSizeInMB > 100) {
                    throw new IllegalArgumentException("고용량 미디어 파일은 100MB 이하만 업로드 가능합니다. 파일: " + originalFilename);
                } else if (!isMedia && fileSizeInMB > 10) {
                    throw new IllegalArgumentException("문서 및 이미지 파일은 10MB 이하만 업로드 가능합니다. 파일: " + originalFilename);
                }

                // --- 파일 저장 ---
                // ✅ [변경] 이름 충돌 방지용 UUID prefix 추가
                // 기존:
                // String fileName = originalFilename;
                // Path filePath = uploadPath.resolve(fileName);
                String storedFileName = java.util.UUID.randomUUID() + "_" + originalFilename;
                java.nio.file.Path filePath = uploadPath.resolve(storedFileName);
                file.transferTo(filePath.toFile());

                // ✅ TodolistFile 엔티티 생성 및 투두 추가 (기존 1개 → N개 누적)
                TodolistFile todolistFile = TodolistFile.builder()
                        .fileName(originalFilename)          // 사용자가 업로드한 원래 이름
                        .filePath(filePath.toString())       // 서버에 저장된 실제 경로(UUID 포함)
                        .contentType(file.getContentType())
                        .uploadedAt(LocalDateTime.now())
                        .todolist(todo)
                        .uploader(uploader)
                        .build();

                todo.getFiles().add(todolistFile);
                savedPaths.add(filePath.toString());

                // ✅ 스토리지에도 복사 (파일별 1회씩)
                storageService.copyToStorageImmediately(todo.getTodoId(), filePath.toString());
            }

            // ✅ [유지] 기존 uploadedFilePath도 세팅 (하위 호환용)
            // 여러 개 중 마지막 파일 경로를 한 번만 저장
            if (!savedPaths.isEmpty()) {
                todo.setUploadedFilePath(savedPaths.get(savedPaths.size() - 1));
            }

            // ✅ 완료 처리 로직 (기존 checkAndUpdateCompletion 재사용)
            checkAndUpdateCompletion(todo);

            todo.setTodoTime(LocalDateTime.now());
            todo.setSubmittedAt(LocalDateTime.now());
            todolistRepository.save(todo);

            // ✅ 완료 알림 (기존 로직 유지)
            notificationService.createTodoCompletedNotification(todoId);

            return "파일 업로드 성공: " + savedPaths.size() + "개 파일이 제출되었습니다.";
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage());
        }
    }


    // ✅ 새로운 메서드: 완료 처리 체크
    private void checkAndUpdateCompletion(Todolist todo) {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = todo.getDueDate();

        // 마감일이 없으면 파일이 있으면 완료 처리
        if (dueDate == null) {
            boolean hasFile = !todo.getFiles().isEmpty();
            todo.setTodoChecked(hasFile);
            return;
        }

        // 마감일 이후인 경우
        if (today.isAfter(dueDate)) {
            // 마감일 내에 제출된 파일이 있는지 체크
            boolean hasFileWithinDeadline = todo.getFiles().stream()
                    .anyMatch(file -> {
                        LocalDateTime uploadedAt = file.getUploadedAt();
                        return uploadedAt != null &&
                                !uploadedAt.toLocalDate().isAfter(dueDate);
                    });

            todo.setTodoChecked(hasFileWithinDeadline);
        } else {
            // 마감일 이전이면 파일이 있으면 완료 처리
            boolean hasFile = !todo.getFiles().isEmpty();
            todo.setTodoChecked(hasFile);
        }
    }

    public Resource downloadFile(Long todoId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        validateTeamMembership(todo.getTeamId(), userId);
        String pathStr = todo.getUploadedFilePath();
        if (pathStr == null || pathStr.isBlank()) {
            throw new IllegalArgumentException("해당 투두에는 업로드된 파일이 없습니다.");
        }

        try {
            Path path = Paths.get(pathStr).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("파일을 읽을 수 없습니다: " + pathStr);
            }
        } catch (Exception e) {
            throw new RuntimeException("파일 다운로드 실패: " + e.getMessage());
        }
    }

    public List<CategoryTodoDto> getTodosByCategory(Long teamId, Long userId) {
        validateTeamMembership(teamId, userId);

        List<Todolist> all = todolistRepository.findAllByTeamId(teamId).stream()
                .filter(t -> isVisibleByDeadline(t.getDueDate()))
                .toList();

        Map<Long, List<TodoSummaryDto>> map = all.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.groupingBy(TodoSummaryDto::getCatId));

        return map.entrySet().stream()
                .map(e -> {
                    Long catId = e.getKey();
                    String name = categoryRepository.findById(catId)
                            .map(Category::getCatName)
                            .orElse("Unknown");
                    return new CategoryTodoDto(catId, name, e.getValue());
                })
                .toList();
    }

    public List<TodoSummaryDto> getWeeklyTodos(Long teamId, Long userId) {
        validateTeamMembership(teamId, userId);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEnd   = weekStart.plusDays(6);

        return todolistRepository.findAllByTeamId(teamId).stream()
                .filter(t -> {
                    LocalDate due = t.getDueDate();
                    return due != null && (
                            (!due.isBefore(weekStart) && !due.isAfter(weekEnd))
                                    || (due.isBefore(today) && !today.isAfter(due.plusDays(3)))
                    );
                })
                .map(this::toSummaryDto)
                .toList();
    }

    private TodoSummaryDto toSummaryDto(Todolist t) {
        String categoryName = categoryRepository.findById(t.getCatId())
                .map(Category::getCatName)
                .orElse("Unknown");
        // ✅ 담당자 id 리스트로 변환
        List<Long> assigneeIds = t.getAssignees().stream()
                .map(User::getUserId)
                .toList();

        List<String> assigneeNames = t.getAssignees().stream()
                .map(User::getUserName)
                .toList();

        // ✅ 생성자 id 추출
        Long creatorId = t.getCreator() != null ? t.getCreator().getUserId() : null;

        return new TodoSummaryDto(
                t.getTodoId(),
                t.getTodoTitle(),
                t.getTodoDes(),
                t.getDueDate(),
                t.getTodoChecked(),
                t.getFileForm(),
                t.getUploadedFilePath(),
                t.getCatId(),
                categoryName,
                assigneeIds,    // List<Long>
                assigneeNames,
                creatorId// Long
        );
    }

    public List<TodoSummaryDto> getTodosByCategoryId(Long teamId, Long catId, Long userId) {
        validateTeamMembership(teamId, userId);
        return todolistRepository
                .findAllByTeamIdAndCatId(teamId, catId)
                .stream()
                .filter(t -> isVisibleByDeadline(t.getDueDate()))
                .map(this::toSummaryDto)
                .toList();
    }

    public List<Todolist> getTodosByConvertStatus(String status, Long userId) {
        return todolistRepository.findAllByConvertStatus(status).stream()
                .filter(todo ->
                        (todo.getCreator() != null && todo.getCreator().getUserId().equals(userId)) ||
                                (todo.getAssignees() != null && todo.getAssignees().stream()
                                        .anyMatch(user -> user.getUserId().equals(userId)))
                )
                .collect(Collectors.toList());
    }

    public List<Todolist> getTodosByTeamAndStatus(Long teamId, String status, Long userId) {
        validateTeamMembership(teamId, userId);
        return todolistRepository.findAllByTeamIdAndConvertStatus(teamId, status);
    }

    public void deleteUploadedFile(Long todoId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        // ✅ 권한 체크: 담당자가 있으면 담당자만, 없으면 팀원 누구나
        if (todo.getAssignees() != null && !todo.getAssignees().isEmpty()) {
            boolean isAssignee = todo.getAssignees().stream()
                    .anyMatch(user -> user.getUserId().equals(userId));

            if (!isAssignee) {
                throw new AccessDeniedException("TODO 담당자만 파일을 삭제할 수 있습니다.");
            }
        } else {
            validateTeamMembership(todo.getTeamId(), userId);
        }

        String pathStr = todo.getUploadedFilePath();
        if (pathStr == null || pathStr.isBlank()) {
            throw new IllegalArgumentException("업로드된 파일 경로가 존재하지 않습니다.");
        }

        Path path = Paths.get(pathStr);
        try {
            Files.deleteIfExists(path);
            storageService.deleteOldTodoFiles(todoId, pathStr);

            // ✅ 파일 목록에서도 제거
            todo.getFiles().removeIf(file -> file.getFilePath().equals(pathStr));

            todo.setUploadedFilePath(null);

            // ✅ 완료 상태 재계산
            checkAndUpdateCompletion(todo);

            todolistRepository.save(todo);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + e.getMessage());
        }
    }

    public void deleteConvertedFile(Long todoId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        boolean isAssignee = todo.getAssignees().stream()
                .anyMatch(user -> user.getUserId().equals(userId));

        if (!isAssignee) {
            throw new AccessDeniedException("TODO 담당자만 변환된 파일을 삭제할 수 있습니다.");
        }

        String pathStr = todo.getConvertedFileUrl();
        if (pathStr == null || pathStr.isBlank()) {
            throw new IllegalArgumentException("변환된 파일 경로가 존재하지 않습니다.");
        }

        Path path = Paths.get(pathStr);
        try {
            Files.deleteIfExists(path);
            todo.setConvertedFileUrl(null);
            todo.setConvertStatus(null);
            todo.setConvertedAt(null);
            todolistRepository.save(todo);
        } catch (IOException e) {
            throw new RuntimeException("변환 파일 삭제 실패: " + e.getMessage());
        }
    }

    public List<TeamWeeklyTodoDto> getMyWeeklyTodosByUser(Long userId) {
        LocalDate today     = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEnd   = weekStart.plusDays(6);

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<Teammates> myTeams = teammatesRepository.findAllByUserId(userId);

        return myTeams.stream()
                .map(tm -> {
                    Long teamId     = tm.getTeamId();
                    Long assigneeId = tm.getTeammatesId();

                    List<WeekTodoDto> todos = todolistRepository
                            .findAllByTeamIdAndDueDateBetween( // (AssigneeId가 빠진 메서드 필요)
                                    teamId, weekStart, weekEnd
                            )
                            .stream()
                            // 2. 'assignees' 목록에 현재 유저가 포함된 것만 필터링
                            .filter(t -> t.getAssignees().contains(currentUser))
                            .map(t -> {
                                // ... (이하 map 로직 동일)
                                String catName = categoryRepository.findById(t.getCatId())
                                        .map(Category::getCatName)
                                        .orElse("Unknown");
                                return new WeekTodoDto(
                                        t.getTodoId(),
                                        t.getCatId(),
                                        catName,
                                        t.getTeamId(),
                                        t.getAssigneeId(), // 🚨 이 필드는 WeekTodoDto에서 제거하거나 null 처리 필요
                                        t.getTodoTitle(),
                                        t.getTodoDes(),
                                        t.getTodoChecked(),
                                        t.getDueDate()
                                );
                            })
                            .toList();

                    String teamName = teamRepository.findById(teamId)
                            .map(Team::getTeamName)
                            .orElse("Unknown");

                    return new TeamWeeklyTodoDto(teamId, teamName, todos);
                })
                .filter(dto -> !dto.getTodos().isEmpty())
                .toList();
    }

    public List<TodoCalendarSummaryDto> getMyTodosForCalendar(Long userId) {
        // 1. 현재 유저 객체를 찾습니다.
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 2. Repository 메서드 변경: `findAllByAssigneeId` 대신 `findAllByAssigneesContaining`
        List<Todolist> myTodos = todolistRepository.findAllByAssigneesContaining(currentUser);

        return myTodos.stream()
                .map(t -> new TodoCalendarSummaryDto(
                        t.getTeamId(),
                        t.getTodoId(),
                        t.getTodoTitle(),
                        t.getDueDate(),
                        true
                ))
                .toList();
    }

}


