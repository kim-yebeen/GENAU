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
import java.util.stream.Collectors;
import com.example.genau.notice.service.NotificationService;

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

    public Todolist createTodolist(TodolistCreateRequest request, Long userId) {
        validateTeamMembership(request.getTeamId(), userId);

        Todolist todo = new Todolist();
        todo.setTeamId(request.getTeamId());
        todo.setCatId(request.getCatId());
        todo.setTodoTitle(request.getTodoTitle());
        todo.setTodoDes(request.getTodoDes());
        todo.setDueDate(request.getDueDate());
        todo.setTodoTime(LocalDateTime.now());
        todo.setFileForm(request.getFileForm());
        todo.setTodoChecked(false);
        todo.setCreatorId(userId);
        todo.setAssigneeId(request.getAssigneeId());

        return todolistRepository.save(todo);
    }

    public Todolist updateTodolist(Long todoId, TodolistUpdateRequest request, Long userId) {

        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (!todo.getCreatorId().equals(userId) && !todo.getAssigneeId().equals(userId)) {
            throw new AccessDeniedException("TODO 생성자 또는 담당자만 수정할 수 있습니다.");
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = todo.getDueDate();

        if (dueDate != null && today.isAfter(dueDate.plusDays(3))) {
            throw new IllegalStateException("마감일이 지난 지 3일이 넘어 수정할 수 없습니다.");
        }

        // DB 업데이트 전에 변경될 제목을 임시 저장
        String oldTitle = todo.getTodoTitle();
        String newTitle = request.getTodoTitle() != null ? request.getTodoTitle() : oldTitle;

        if (dueDate != null && today.isAfter(dueDate)) {
            todo.setFileForm(request.getFileForm());
        } else {
            todo.setTodoTitle(request.getTodoTitle());
            todo.setTodoDes(request.getTodoDes());
            todo.setDueDate(request.getDueDate());
            todo.setFileForm(request.getFileForm());
            todo.setAssigneeId(request.getAssigneeId());
        }

        todo.setTodoTime(LocalDateTime.now());

        Todolist savedTodo = todolistRepository.save(todo);

        if (request.getTodoTitle() != null) {
            String message = String.format(
                    "{\"type\":\"TODO_UPDATED\", \"todoId\":%d, \"newTitle\":\"%s\"}",
                    savedTodo.getTodoId(), savedTodo.getTodoTitle()
            );
            todoUpdateHandler.broadcast(message);
        }

        return savedTodo; // ✅ 중복 save() 제거
    }

    public void deleteTodolist(Long todoId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (!todo.getCreatorId().equals(userId)) {
            throw new AccessDeniedException("TODO 생성자만 삭제할 수 있습니다.");
        }
        todolistRepository.deleteById(todoId);
    }

    public Todolist updateTodolistWithFile(Long todoId, TodolistUpdateRequest request, Long userId, MultipartFile newFile) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));


        // 권한 체크

        if (!todo.getCreatorId().equals(userId) && !todo.getAssigneeId().equals(userId)) {
            throw new AccessDeniedException("TODO 생성자 또는 담당자만 수정할 수 있습니다.");
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = todo.getDueDate();

        if (newFile != null && !newFile.isEmpty()) {
            if (dueDate != null && today.isAfter(dueDate)) {
                throw new IllegalStateException("마감일이 지난 후에는 파일을 변경할 수 없습니다.");
            }

            String oldFilePath = todo.getUploadedFilePath();

            try {

                // ✅ 1. 먼저 스토리지에서 기존 파일 삭제 (새 파일 업로드 전에)

                if (oldFilePath != null && !oldFilePath.isBlank()) {
                    storageService.deleteOldTodoFiles(todoId, oldFilePath);
                }

                // ✅ 2. uploads 폴더에서도 기존 파일 삭제
                if (oldFilePath != null && !oldFilePath.isBlank()) {
                    java.nio.file.Path oldFile = java.nio.file.Paths.get(oldFilePath);
                    java.nio.file.Files.deleteIfExists(oldFile);
                    System.out.println("기존 uploads 파일 삭제: " + oldFile);
                }


                // ✅ 3. 새 파일 업로드

                String uploadDir = System.getProperty("user.dir") + "/uploads";
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);

                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }

                String fileName = newFile.getOriginalFilename();
                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                newFile.transferTo(filePath.toFile());

                // ✅ 4. DB 업데이트
                todo.setUploadedFilePath(filePath.toString());
                todo.setTodoTime(LocalDateTime.now());
                todolistRepository.save(todo);

                // ✅ 5. 새 파일을 스토리지에 복사

                todo.setUploadedFilePath(filePath.toString());
                todo.setTodoTime(LocalDateTime.now());

                boolean hasFile = todo.getUploadedFilePath() != null && !todo.getUploadedFilePath().isBlank();
                todo.setTodoChecked(hasFile);

                todolistRepository.save(todo);
                storageService.copyToStorageImmediately(todoId, filePath.toString());

            } catch (Exception e) {
                throw new RuntimeException("파일 수정 실패: " + e.getMessage());
            }
        }

        // 다른 필드 업데이트가 있는 경우에만 기존 메서드 호출

        if (request != null && hasNonNullFields(request)) {
            return updateTodolist(todoId, request, userId);
        }

        return todo;
    }

    // ✅ request에 null이 아닌 필드가 있는지 확인하는 헬퍼 메서드

    private boolean hasNonNullFields(TodolistUpdateRequest request) {
        return request.getTodoTitle() != null ||
                request.getTodoDes() != null ||
                request.getDueDate() != null ||
                request.getFileForm() != null ||
                request.getAssigneeId() != null;
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

    public String submitFile(Long todoId, Long userId, MultipartFile file) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (!todo.getAssigneeId().equals(userId)) {
            throw new IllegalArgumentException("이 투두는 해당 팀원에게 할당되지 않았습니다.");
        }

        LocalDate today = LocalDate.now();
        if (todo.getDueDate() != null && today.isAfter(todo.getDueDate().plusDays(3))) {
            throw new IllegalStateException("마감일이 지난 지 3일이 지나 업로드할 수 없습니다.");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 이름에 확장자가 없습니다.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        List<String> allowedExtensions = List.of(
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "txt", "md", "csv", "jpg", "jpeg", "png", "gif"
        );

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않은 파일 확장자입니다: " + extension);
        }

        long fileSizeInMB = file.getSize() / (1024 * 1024);
        boolean isMedia = List.of("mp3", "wav", "mp4", "avi", "mov").contains(extension);

        if (isMedia && fileSizeInMB > 100) {
            throw new IllegalArgumentException("고용량 미디어 파일은 100MB 이하만 업로드 가능합니다.");
        } else if (!isMedia && fileSizeInMB > 10) {
            throw new IllegalArgumentException("문서 및 이미지 파일은 10MB 이하만 업로드 가능합니다.");
        }

        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);

            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }


            /// ✅ 3. 원본 파일명 그대로 사용 (todoId 제거)

            String fileName = originalFilename;
            java.nio.file.Path filePath = uploadPath.resolve(fileName);

            file.transferTo(filePath.toFile());

            todo.setUploadedFilePath(filePath.toString());

            boolean hasFile = todo.getUploadedFilePath() != null && !todo.getUploadedFilePath().isBlank();
            todo.setTodoChecked(hasFile);

            todo.setTodoTime(LocalDateTime.now());
            todo.setSubmittedAt(LocalDateTime.now());
            todolistRepository.save(todo);


            //storageService.moveToStorageIfConfirmed(todo.getTodoId());

            storageService.copyToStorageImmediately(todo.getTodoId(), filePath.toString());

            notificationService.createTodoCompletedNotification(todoId);

            return "파일 업로드 성공: " + filePath;
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage());
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
                t.getAssigneeId(),
                t.getCreatorId()
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
                .filter(todo -> todo.getCreatorId().equals(userId) || todo.getAssigneeId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Todolist> getTodosByTeamAndStatus(Long teamId, String status, Long userId) {
        validateTeamMembership(teamId, userId);
        return todolistRepository.findAllByTeamIdAndConvertStatus(teamId, status);
    }

    // ✅ 파일 삭제 시 스토리지 파일도 함께 삭제 (여기만 추가)
    public void deleteUploadedFile(Long todoId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (!todo.getAssigneeId().equals(userId)) {
            throw new AccessDeniedException("TODO 담당자만 파일을 삭제할 수 있습니다.");
        }

        String pathStr = todo.getUploadedFilePath();
        if (pathStr == null || pathStr.isBlank()) {
            throw new IllegalArgumentException("업로드된 파일 경로가 존재하지 않습니다.");
        }

        Path path = Paths.get(pathStr);
        try {
            Files.deleteIfExists(path);

            // ✅ 스토리지 파일도 삭제 (이 한 줄만 추가)
            storageService.deleteOldTodoFiles(todoId, pathStr);

            todo.setUploadedFilePath(null);
            boolean hasFile = todo.getUploadedFilePath() != null && !todo.getUploadedFilePath().isBlank();
            todo.setTodoChecked(hasFile);

            todolistRepository.save(todo);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + e.getMessage());
        }
    }

    public void deleteConvertedFile(Long todoId, Long userId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (!todo.getAssigneeId().equals(userId)) {
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

        List<Teammates> myTeams = teammatesRepository.findAllByUserId(userId);

        return myTeams.stream()
                .map(tm -> {
                    Long teamId     = tm.getTeamId();
                    Long assigneeId = tm.getTeammatesId();

                    List<WeekTodoDto> todos = todolistRepository
                            .findAllByTeamIdAndAssigneeIdAndDueDateBetween(
                                    teamId, userId, weekStart, weekEnd
                            )
                            .stream()
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
        List<Todolist> myTodos = todolistRepository.findAllByAssigneeId(userId);

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


