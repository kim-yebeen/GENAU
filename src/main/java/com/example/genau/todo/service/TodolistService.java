package com.example.genau.todo.service;

import com.example.genau.category.domain.Category;
import com.example.genau.category.repository.CategoryRepository;
import com.example.genau.todo.dto.CategoryTodoDto;
import com.example.genau.todo.dto.TodoSummaryDto;
import com.example.genau.todo.dto.TodolistCreateRequest;
import com.example.genau.todo.dto.TodolistUpdateRequest; // ✅ 추가
import com.example.genau.team.domain.Teammates;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
import com.example.genau.todo.dto.*;
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.repository.TodolistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.lang.Exception;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.example.genau.team.domain.Team;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Optional;// ✅ 추가
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

    private final NotificationService      notificationService;

    public List<Todolist> getTodosByTeamId(Long teamId) {
        return todolistRepository.findAllByTeamId(teamId);
    }

    public Todolist createTodolist(TodolistCreateRequest request) {
        Todolist todo = new Todolist();
        todo.setTeamId(request.getTeamId());
        todo.setCatId(request.getCatId());
        todo.setTodoTitle(request.getTodoTitle());
        todo.setTodoDes(request.getTodoDes());
        todo.setDueDate(request.getDueDate());
        todo.setTodoTime(LocalDateTime.now());
        todo.setFileForm(request.getFileForm());
        todo.setTodoChecked(false);
        todo.setCreatorId(request.getCreatorId());
        todo.setAssigneeId(request.getAssigneeId());

        return todolistRepository.save(todo);
    }

    // ✅ 여기 추가: 투두 수정 메서드
    public Todolist updateTodolist(Long todoId, TodolistUpdateRequest request) {
        Optional<Todolist> optionalTodo = todolistRepository.findById(todoId);

        if (optionalTodo.isEmpty()) {
            throw new IllegalArgumentException("Todo not found with id: " + todoId);
        }

        Todolist todo = optionalTodo.get();
        todo.setTodoTitle(request.getTodoTitle());
        todo.setTodoDes(request.getTodoDes());
        todo.setDueDate(request.getDueDate());
        todo.setTodoTime(LocalDateTime.now());

        return todolistRepository.save(todo);
    }

    // ✅ 여기 추가: 투두 삭제 메서드
    public void deleteTodolist(Long todoId) {
        if (!todolistRepository.existsById(todoId)) {
            throw new IllegalArgumentException("Todo not found with id: " + todoId);
        }
        todolistRepository.deleteById(todoId);
    }

    // ✅ 여기 추가: 파일 확장자 검증 메서드
    public boolean validateFileExtension(Long todoId, MultipartFile file) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        String requiredExtension = todo.getFileForm(); // 요구하는 확장자 (ex: "pdf")
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 이름에 확장자가 없습니다.");
        }

        String submittedExtension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);

        return requiredExtension.equalsIgnoreCase(submittedExtension);
    }

    // ✅ TodolistService 클래스 안에 추가

    public String verifyFile(Long todoId, MultipartFile file) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        String requiredExtension = todo.getFileForm(); // 요구하는 확장자 (ex: "pdf")

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

    public String submitFile(Long todoId, Long teammatesId, MultipartFile file) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (!todo.getAssigneeId().equals(teammatesId)) {
            throw new IllegalArgumentException("이 투두는 해당 팀원에게 할당되지 않았습니다.");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 이름에 확장자가 없습니다.");
        }

        // ✅ 확장자 추출 및 허용 여부 검사
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        List<String> allowedExtensions = List.of(
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "txt", "md", "csv", "jpg", "jpeg", "png", "gif"
        );

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않은 파일 확장자입니다: " + extension);
        }

        // ✅ 파일 크기 제한 검사
        long fileSizeInMB = file.getSize() / (1024 * 1024);
        boolean isMedia = List.of("mp3", "wav", "mp4", "avi", "mov").contains(extension);

        if (isMedia && fileSizeInMB > 100) {
            throw new IllegalArgumentException("고용량 미디어 파일은 100MB 이하만 업로드 가능합니다.");
        } else if (!isMedia && fileSizeInMB > 10) {
            throw new IllegalArgumentException("문서 및 이미지 파일은 10MB 이하만 업로드 가능합니다.");
        }

        try {
            //String originalFilename = file.getOriginalFilename();

            // 1. 프로젝트 루트 기준으로 절대경로 설정
            String uploadDir = System.getProperty("user.dir") + "/uploads";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);

            // 2. uploads 폴더 없으면 생성
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            // 3. 저장 파일 경로 설정
            String fileName = todoId + "_" + originalFilename;
            java.nio.file.Path filePath = uploadPath.resolve(fileName);

            // 4. 실제 파일 저장
            file.transferTo(filePath.toFile());

            // DB에 저장
            todo.setUploadedFilePath(filePath.toString());

            //제출 완료 처리 <--예빈 추가..
            todo.setTodoChecked(true);

            todo.setTodoTime(LocalDateTime.now()); // 제출 시각 저장
            // todo.setSubmitStatus("SUBMITTED"); // 상태 관리가 필요하면
            todo.setSubmittedAt(LocalDateTime.now());
            todolistRepository.save(todo);

            // 완료 알림 보내기 <-- 예빈 추가
            notificationService.createTodoCompletedNotification(todoId);

            return "파일 업로드 성공: " + filePath;
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage());
        }
    }

    public Resource downloadFile(Long todoId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

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

    /** 카테고리별 할 일 그룹핑 */
    public List<CategoryTodoDto> getTodosByCategory(Long teamId) {
        List<Todolist> all = todolistRepository.findAllByTeamId(teamId);

        // 그룹핑: catId → List<TodoSummaryDto>
        Map<Long, List<TodoSummaryDto>> map = all.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.groupingBy(TodoSummaryDto::getCatId));

        // DTO 로 변환
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

    /** 이번 주 할 일 (dueDate 오늘 ~ 7일 이내) */
    public List<TodoSummaryDto> getWeeklyTodos(Long teamId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEnd   = weekStart.plusDays(6);

        return todolistRepository.findAllByTeamId(teamId).stream()
                .filter(t -> {
                    LocalDate due = t.getDueDate();
                    return due != null
                            && !due.isBefore(weekStart)
                            && !due.isAfter(weekEnd);
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
                t.getCatId(),
                categoryName
        );
    }

    /**특정 카테고리만 뽑아주는 메서드**/
    public List<TodoSummaryDto> getTodosByCategoryId(Long teamId, Long catId) {
        return todolistRepository
                .findAllByTeamIdAndCatId(teamId, catId)
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public List<Todolist> getTodosByConvertStatus(String status) {
        return todolistRepository.findAllByConvertStatus(status);
    }

    public List<Todolist> getTodosByTeamAndStatus(Long teamId, String status) {
        return todolistRepository.findAllByTeamIdAndConvertStatus(teamId, status);
    }

    public void deleteUploadedFile(Long todoId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        String pathStr = todo.getUploadedFilePath();
        if (pathStr == null || pathStr.isBlank()) {
            throw new IllegalArgumentException("업로드된 파일 경로가 존재하지 않습니다.");
        }

        Path path = Paths.get(pathStr);
        try {
            Files.deleteIfExists(path);
            todo.setUploadedFilePath(null); // DB 경로도 초기화
            todolistRepository.save(todo);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + e.getMessage());
        }
    }

    public void deleteConvertedFile(Long todoId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        String pathStr = todo.getConvertedFileUrl();
        if (pathStr == null || pathStr.isBlank()) {
            throw new IllegalArgumentException("변환된 파일 경로가 존재하지 않습니다.");
        }

        Path path = Paths.get(pathStr);
        try {
            Files.deleteIfExists(path);
            todo.setConvertedFileUrl(null);     // 경로 초기화
            todo.setConvertStatus(null);        // 상태 초기화
            todo.setConvertedAt(null);          // 변환 시점 초기화
            todolistRepository.save(todo);
        } catch (IOException e) {
            throw new RuntimeException("변환 파일 삭제 실패: " + e.getMessage());
        }
    }



    /**
     * 내 계정(userId)이 속한 모든 팀별로,
     * 이번 주(일요일~토요일)에 나(teammatesId)에게 할당된
     * TODO 목록을 카테고리명과 함께 반환
     */
    public List<TeamWeeklyTodoDto> getMyWeeklyTodosByUser(Long userId) {
        LocalDate today     = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEnd   = weekStart.plusDays(6);

        // 1) 내가 속한 팀 목록 조회
        List<Teammates> myTeams = teammatesRepository.findAllByUserId(userId);

        return myTeams.stream()
                .map(tm -> {
                    Long teamId     = tm.getTeamId();
                    Long assigneeId = tm.getTeammatesId();

                    // 2) 팀·담당자·기간으로 TODO 조회
                    List<WeekTodoDto> todos = todolistRepository
                            .findAllByTeamIdAndAssigneeIdAndDueDateBetween(
                                    teamId, userId, weekStart, weekEnd
                            )
                            .stream()
                            .map(t -> {
                                // 카테고리명 조회
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

                    // 3) 팀명 조회
                    String teamName = teamRepository.findById(teamId)
                            .map(Team::getTeamName)
                            .orElse("Unknown");

                    return new TeamWeeklyTodoDto(teamId, teamName, todos);
                })
                // 4) TODO가 하나도 없는 팀은 제외
                .filter(dto -> !dto.getTodos().isEmpty())
                .toList();
    }
}





