package com.example.genau.todo.controller;

import com.example.genau.user.security.AuthUtil;
import com.example.genau.todo.dto.*;
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.service.FileConvertService;
import com.example.genau.todo.service.TodolistService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;



@RestController
@RequestMapping("/todos")
public class TodolistController {

    private final TodolistService todolistService;
    private final FileConvertService fileConvertService;

    public TodolistController(TodolistService todolistService, FileConvertService fileConvertService) {
        this.todolistService = todolistService;
        this.fileConvertService = fileConvertService;
    }

    @PostMapping
    public Todolist createTodolist(@RequestBody TodolistCreateRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return todolistService.createTodolist(request,userId);
    }

    // todo modify
    @PatchMapping("/{todoId}")
    public Todolist updateTodolist(@PathVariable Long todoId, @RequestBody TodolistUpdateRequest request) {
        Long userId=AuthUtil.getCurrentUserId();
        return todolistService.updateTodolist(todoId, request, userId);
    }

    // todo delete
    @DeleteMapping("/{todoId}")
    public void deleteTodolist(@PathVariable Long todoId) {
        Long userId= AuthUtil.getCurrentUserId();
        todolistService.deleteTodolist(todoId, userId);
    }

    // todo get
    @GetMapping("/team/{teamId}")
    public List<Todolist> getTodosByTeamId(@PathVariable Long teamId) {
        Long userId = AuthUtil.getCurrentUserId();
        return todolistService.getTodosByTeamId(teamId, userId);
    }

    //투두 완료 체크
    @PutMapping("/{todoId}/check")
    public ResponseEntity<Void> updateTodoCheckStatus(
            @PathVariable Long todoId,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean checked = body.getOrDefault("todoChecked", false);
        todolistService.updateTodoChecked(todoId, checked);
        return ResponseEntity.ok().build();
    }

    // file verify
    @PostMapping("/{todoId}/verify")
    public String verifyFile(
            @PathVariable Long todoId,
            @RequestParam("file") MultipartFile file
    ) {
        return todolistService.verifyFile(todoId, file);
    }

    // file submit
    @PostMapping("/{todoId}/submit")
    public ResponseEntity<String> submitFile(
            @PathVariable Long todoId,
            @RequestParam("file") MultipartFile file) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            String result = todolistService.submitFile(todoId, userId, file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("요청 오류: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 오류: " + e.getMessage());
        }
    }

    //file download
    @GetMapping("/{todoId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long todoId) {
        Long userId = AuthUtil.getCurrentUserId();
        Resource resource = todolistService.downloadFile(todoId, userId);
        String filename = resource.getFilename();

        // UTF-8로 인코딩 (한글 등 비 ASCII 대응)
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .body(resource);
    }

    /** 1) 카테고리별 할 일 조회 */
    @GetMapping("/team/{teamId}/by-category")
    public List<CategoryTodoDto> getByCategory(@PathVariable Long teamId) {
        Long userId = AuthUtil.getCurrentUserId();
        return todolistService.getTodosByCategory(teamId, userId);
    }

    /** 2) 이번 주(일~토) 할 일 조회 */
    @GetMapping("/team/{teamId}/weekly")
    public List<TodoSummaryDto> getWeekly(@PathVariable Long teamId) {
        Long userId = AuthUtil.getCurrentUserId();
        return todolistService.getWeeklyTodos(teamId, userId);
    }

    /** 3)특정 카테고리 할 일 조회**/
    @GetMapping("team/{teamId}/category/{catId}")
    public ResponseEntity<List<TodoSummaryDto>> getByCategoryId(
            @PathVariable Long teamId,
            @PathVariable Long catId
    ) {
        Long userId = AuthUtil.getCurrentUserId();
        return ResponseEntity.ok(
                todolistService.getTodosByCategoryId(teamId, catId, userId)
        );
    }

    @PostMapping("/{todoId}/convert")
    public ResponseEntity<Resource> convertFile(
            @PathVariable Long todoId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat
    ){
        try {
            Long userId = AuthUtil.getCurrentUserId();
            Resource result = fileConvertService.convertFile(file, targetFormat, todoId, userId);
            String filename = result.getFilename();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                    .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                    .body(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // 전체 변환 상태별 조회
    @GetMapping("/convert/status")
    public List<Todolist> getByConvertStatus(@RequestParam("status") String status) {
        Long userId = AuthUtil.getCurrentUserId();
        return todolistService.getTodosByConvertStatus(status, userId);
    }

    // 팀별 변환 상태 조회
    @GetMapping("/team/{teamId}/convert/status")
    public List<Todolist> getByTeamAndConvertStatus(
            @PathVariable Long teamId,
            @RequestParam("status") String status
    ) {
        Long userId = AuthUtil.getCurrentUserId();
        return todolistService.getTodosByTeamAndStatus(teamId, status, userId);
    }

    // 업로드 파일 삭제
    @DeleteMapping("/{todoId}/upload-file")
    public ResponseEntity<String> deleteUploadedFile(@PathVariable Long todoId) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            todolistService.deleteUploadedFile(todoId, userId);
            return ResponseEntity.ok("업로드된 파일이 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 삭제 실패: " + e.getMessage());
        }
    }

    // 변환된 파일 삭제
    @DeleteMapping("/{todoId}/converted-file")
    public ResponseEntity<String> deleteConvertedFile(@PathVariable Long todoId) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            todolistService.deleteConvertedFile(todoId, userId);
            return ResponseEntity.ok("변환된 파일이 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("변환 파일 삭제 실패: " + e.getMessage());
        }
    }

    //내 이번주 할일 목록 조회
    @GetMapping("/me/weekly")
    public List<TeamWeeklyTodoDto> getMyWeeklyTodosByUser() {
        Long userId = AuthUtil.getCurrentUserId();
        return todolistService.getMyWeeklyTodosByUser(userId);
    }

    //전체할일목록조회(내할당, 캘린더바인딩용)
    @GetMapping("/me/calendar-todos")
    public ResponseEntity<List<TodoCalendarSummaryDto>> getMyCalendarTodos() {
        Long userId = AuthUtil.getCurrentUserId();
        return ResponseEntity.ok(todolistService.getMyTodosForCalendar(userId));
    }
}


