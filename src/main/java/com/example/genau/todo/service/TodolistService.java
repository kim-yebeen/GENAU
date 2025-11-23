
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
        todo.setTodoChecked(false);

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));
        todo.setCreator(creator);

        if (request.getAssigneeIds() != null && !request.getAssigneeIds().isEmpty()) {
            List<User> users = request.getAssigneeIds().stream()
                    .map(id -> userRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id)))
                    .collect(Collectors.toList());
            todo.setAssignees(users);
        }

        if (request.getFileForm() != null && !request.getFileForm().isBlank()) {
            todo.setFileForm(request.getFileForm());
        }

        List<TodolistFile> savedFiles = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String ext = FileValidationUtil.getExtension(file.getOriginalFilename());
                    if (todo.getFileForm() != null && !todo.getFileForm().equalsIgnoreCase(ext)) {
                        throw new IllegalArgumentException("파일 확장자가 요구 형식과 일치하지 않습니다: " + ext);
                    }

                    String uploadPath = fileStorageService.saveFile(file);
                    TodolistFile todolistFile = TodolistFile.builder()
                            .fileName(file.getOriginalFilename())
                            .filePath(uploadPath)
                            .contentType(file.getContentType())
                            .uploadedAt(LocalDateTime.now())
                            .todolist(todo)
                            .uploader(creator)
                            .convertStatus("WAITING")
                            .build();

                    savedFiles.add(todolistFile);
                }
            }
            todo.setFiles(savedFiles);
        }

        Todolist savedTodo = todolistRepository.save(todo);

        if (!savedFiles.isEmpty()) {
            for (int i = 0; i < savedFiles.size(); i++) {
                TodolistFile savedFile = savedTodo.getFiles().get(i);
                MultipartFile originalFile = files.get(i);

                try {
                    fileConvertService.convertToPdf(savedFile, originalFile);
                } catch (Exception e) {
                    System.err.println("파일 변환 실패: " + savedFile.getFileName() + " - " + e.getMessage());
                }
            }
        }

        return savedTodo;
    }

    private void validateFileExtension(String fileForm) {
        if (!allowedExtensions.contains(fileForm.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않은 파일 확장자입니다: " + fileForm);
        }
    }

    public Todolist updateTodolist(Long todoId, TodolistUpdateRequest request, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

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
            if (request.getAssigneeIds() != null) {
                if (request.getAssigneeIds().isEmpty()) {
                    todo.setAssignees(new ArrayList<>());
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

        String[] allowedExtensionsArr = allowedExtensions.toArray(new String[0]);

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
                List<TodolistFile> oldFiles = todo.getFiles();
                for (TodolistFile oldFile : oldFiles) {
                    if (oldFilePath != null && !oldFilePath.isBlank()) {
                        Path oldPath = Paths.get(oldFilePath);
                        Files.deleteIfExists(oldPath);
                        storageService.deleteOldTodoFiles(todoId, oldFilePath);
                    }
                }

                todo.getFiles().clear();

                List<TodolistFile> newFiles = new ArrayList<>();
                String uploadDir = System.getProperty("user.dir") + "/uploads";
                Path uploadPath = Paths.get(uploadDir);
                Files.createDirectories(uploadPath);

                for (MultipartFile file : files) {
                    if (!FileValidationUtil.isValidExtension(file.getOriginalFilename(), allowedExtensionsArr)) {
                        throw new IllegalArgumentException("허용되지 않은 확장자: " + file.getOriginalFilename());
                    }

                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    file.transferTo(filePath.toFile());

                    TodolistFile fileEntity = TodolistFile.builder()
                            .fileName(file.getOriginalFilename())
                            .filePath(filePath.toString())
                            .uploadedAt(LocalDateTime.now())
                            .uploader(userRepository.findById(userId).orElseThrow())
                            .todolist(todo)
                            .build();

                    newFiles.add(fileEntity);
                    storageService.copyToStorageImmediately(todoId, filePath.toString());
                }

                todo.getFiles().addAll(newFiles);
                todo.setTodoTime(LocalDateTime.now());
                todo.setTodoChecked(true);

            } catch (Exception e) {
                throw new RuntimeException("파일 수정 실패: " + e.getMessage());
            }
        }

        if (request != null && hasNonNullFields(request)) {
            return updateTodolist(todoId, request, userId);
        }

        return todolistRepository.save(todo);
    }

    private boolean hasNonNullFields(TodolistUpdateRequest request) {
        return request.getTodoTitle() != null ||
                request.getTodoDes() != null ||
                request.getDueDate() != null ||
                request.getFileForm() != null ||
                request.getAssigneeIds() != null;
    }

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

    @Transactional
    public List<TodolistFile> submitFiles(Long todoId, Long userId, List<MultipartFile> files) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (todo.getAssignees() != null && !todo.getAssignees().isEmpty()) {
            boolean isAssignee = todo.getAssignees().stream()
                    .anyMatch(user -> user.getUserId().equals(userId));
            if (!isAssignee) {
                throw new AccessDeniedException("파일은 담당자만 제출할 수 있습니다.");
            }
        } else {
            validateTeamMembership(todo.getTeamId(), userId);
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = todo.getDueDate();
        if (dueDate != null && today.isAfter(dueDate.plusDays(3))) {
            throw new IllegalStateException("마감일이 지난 지 3일이 넘어 업로드할 수 없습니다.");
        }

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<TodolistFile> uploadedFiles = new ArrayList<>();
        List<MultipartFile> originalFiles = new ArrayList<>();
        String uploadDir = System.getProperty("user.dir") + "/uploads";

        try {
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            // 각 파일 처리
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || !originalFilename.contains(".")) {
                    throw new IllegalArgumentException("파일 이름에 확장자가 없습니다: " + originalFilename);
                }

                // 확장자 검증
                String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
                if (!allowedExtensions.contains(extension)) {
                    throw new IllegalArgumentException("허용되지 않은 파일 확장자입니다: " + extension);
                }

                // 파일 크기 검증
                long fileSizeInMB = file.getSize() / (1024 * 1024);
                boolean isMedia = List.of("mp3", "wav", "mp4", "avi", "mov").contains(extension);

                if (isMedia && fileSizeInMB > 100) {
                    throw new IllegalArgumentException("고용량 미디어 파일은 100MB 이하만 업로드 가능합니다.");
                } else if (!isMedia && fileSizeInMB > 10) {
                    throw new IllegalArgumentException("문서 및 이미지 파일은 10MB 이하만 업로드 가능합니다.");
                }

                // 파일 저장
                String fileName = System.currentTimeMillis() + "_" + originalFilename;
                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                file.transferTo(filePath.toFile());

                // TodolistFile 엔티티 생성
                TodolistFile todolistFile = TodolistFile.builder()
                        .fileName(originalFilename)
                        .filePath(filePath.toString())
                        .contentType(file.getContentType())
                        .uploadedAt(LocalDateTime.now())
                        .todolist(todo)
                        .uploader(uploader)
                        .build();

                uploadedFiles.add(todolistFile);
                originalFiles.add(file);
                todo.getFiles().add(todolistFile);
            }

            Todolist savedTodo = todolistRepository.save(todo);

            // 변환 처리 로직
            String requiredFormat = savedTodo.getFileForm();

            if (requiredFormat != null && !requiredFormat.isEmpty()) {
                for (int i = 0; i < uploadedFiles.size(); i++) {
                    TodolistFile savedFile = savedTodo.getFiles().get(savedTodo.getFiles().size() - uploadedFiles.size() + i);
                    MultipartFile originalFile = originalFiles.get(i);

                    String extension = savedFile.getFileName()
                            .substring(savedFile.getFileName().lastIndexOf('.') + 1)
                            .toLowerCase();

                    if (requiredFormat.equalsIgnoreCase(extension)) {
                        savedFile.setConvertStatus(null);
                        copyToStorage(savedTodo, savedFile);
                    } else {
                        savedFile.setConvertStatus("WAITING");
                        try {
                            fileConvertService.convertToFormat(savedFile, originalFile, requiredFormat);
                        } catch (Exception e) {
                            System.err.println("파일 변환 시작 실패: " + savedFile.getFileName());
                            savedFile.setConvertStatus("FAILED");
                        }
                    }
                }
            } else {
                for (int i = 0; i < uploadedFiles.size(); i++) {
                    TodolistFile savedFile = savedTodo.getFiles().get(savedTodo.getFiles().size() - uploadedFiles.size() + i);
                    savedFile.setConvertStatus(null);
                    copyToStorage(savedTodo, savedFile);
                }
            }

            if (!uploadedFiles.isEmpty()) {
                savedTodo.setUploadedFilePath(uploadedFiles.get(0).getFilePath());
            }

            checkAndUpdateCompletion(savedTodo);

            savedTodo.setTodoTime(LocalDateTime.now());
            savedTodo.setSubmittedAt(LocalDateTime.now());
            todolistRepository.save(savedTodo);

            notificationService.createTodoCompletedNotification(todoId);

            return savedTodo.getFiles().subList(
                    savedTodo.getFiles().size() - uploadedFiles.size(),
                    savedTodo.getFiles().size()
            );

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage());
        }
    }

    private void copyToStorage(Todolist todo, TodolistFile savedFile) throws IOException {
        String storageDir = System.getProperty("user.dir") + "/storage/team-" + todo.getTeamId();
        Path storagePath = Paths.get(storageDir);
        Files.createDirectories(storagePath);

        Path destinationPath = storagePath.resolve(savedFile.getFileName());
        Files.copy(
                Paths.get(savedFile.getFilePath()),
                destinationPath,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );

        savedFile.setConvertedFilePath(destinationPath.toString());
        System.out.println("✅ 원본 파일 storage 복사 완료: " + destinationPath);
    }

    private void checkAndUpdateCompletion(Todolist todo) {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = todo.getDueDate();

        if (dueDate == null) {
            boolean hasFile = !todo.getFiles().isEmpty();
            todo.setTodoChecked(hasFile);
            return;
        }

        if (today.isAfter(dueDate)) {
            boolean hasFileWithinDeadline = todo.getFiles().stream()
                    .anyMatch(file -> {
                        LocalDateTime uploadedAt = file.getUploadedAt();
                        return uploadedAt != null &&
                                !uploadedAt.toLocalDate().isAfter(dueDate);
                    });

            todo.setTodoChecked(hasFileWithinDeadline);
        } else {
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

    public Resource downloadFileById(Long todoId, Long fileId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        validateTeamMembership(todo.getTeamId(), userId);

        TodolistFile file = todo.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File not found with id: " + fileId));

        String pathStr = file.getFilePath();
        if (pathStr == null || pathStr.isBlank()) {
            throw new IllegalArgumentException("파일 경로가 없습니다.");
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

    public Resource downloadConvertedFile(Long todoId, Long fileId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        validateTeamMembership(todo.getTeamId(), userId);

        TodolistFile file = todo.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File not found with id: " + fileId));

        String pathStr = file.getConvertedFilePath();
        if (pathStr == null || pathStr.isBlank()) {
            throw new IllegalArgumentException("변환된 파일이 없습니다.");
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
        List<Long> assigneeIds = t.getAssignees().stream()
                .map(User::getUserId)
                .toList();

        List<String> assigneeNames = t.getAssignees().stream()
                .map(User::getUserName)
                .toList();

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
                assigneeIds,
                assigneeNames,
                creatorId
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

    public void deleteUploadedFileById(Long todoId, Long fileId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (todo.getAssignees() != null && !todo.getAssignees().isEmpty()) {
            boolean isAssignee = todo.getAssignees().stream()
                    .anyMatch(user -> user.getUserId().equals(userId));
            if (!isAssignee) {
                throw new AccessDeniedException("TODO 담당자만 파일을 삭제할 수 있습니다.");
            }
        } else {
            validateTeamMembership(todo.getTeamId(), userId);
        }

        TodolistFile file = todo.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File not found with id: " + fileId));

        String pathStr = file.getFilePath();
        if (pathStr != null && !pathStr.isBlank()) {
            Path path = Paths.get(pathStr);
            try {
                Files.deleteIfExists(path);
                storageService.deleteOldTodoFiles(todoId, pathStr);
            } catch (IOException e) {
                System.err.println("파일 삭제 실패: " + e.getMessage());
            }
        }

        String convertedPath = file.getConvertedFilePath();
        if (convertedPath != null && !convertedPath.isBlank()) {
            try {
                Files.deleteIfExists(Paths.get(convertedPath));
            } catch (IOException e) {
                System.err.println("변환 파일 삭제 실패: " + e.getMessage());
            }
        }

        todo.getFiles().remove(file);
        checkAndUpdateCompletion(todo);
        todolistRepository.save(todo);
    }

    public List<TodolistFile> getTodoFiles(Long todoId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        validateTeamMembership(todo.getTeamId(), userId);

        return todo.getFiles();
    }

    public void deleteUploadedFile(Long todoId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

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
            todo.getFiles().removeIf(file -> file.getFilePath().equals(pathStr));
            todo.setUploadedFilePath(null);
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
                    List<WeekTodoDto> todos = todolistRepository
                            .findAllByTeamIdAndDueDateBetween(
                                    teamId, weekStart, weekEnd
                            )
                            .stream()
                            .filter(t -> t.getAssignees().contains(currentUser))
                            .map(t -> {
                                String catName = categoryRepository.findById(t.getCatId())
                                        .map(Category::getCatName)
                                        .orElse("Unknown");
                                return new WeekTodoDto(
                                        t.getTodoId(),
                                        t.getCatId(),
                                        catName,
                                        t.getTeamId(),
                                        t.getAssigneeId(),
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
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

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