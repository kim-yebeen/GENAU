package com.example.genau.todo.controller;

import com.example.genau.todo.dto.*;
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.service.TodolistService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@RestController
@RequestMapping("/todos")
public class TodolistController {

    private final TodolistService todolistService;

    public TodolistController(TodolistService todolistService) {
        this.todolistService = todolistService;
    }

    @PostMapping
    public Todolist createTodolist(@RequestBody TodolistCreateRequest request) {
        return todolistService.createTodolist(request);
    }

    // ✅ 여기 추가: 투두 수정 API
    @PutMapping("/{todoId}")
    public Todolist updateTodolist(@PathVariable Long todoId, @RequestBody TodolistUpdateRequest request) {
        return todolistService.updateTodolist(todoId, request);
    }

    @DeleteMapping("/{todoId}")
    public void deleteTodolist(@PathVariable Long todoId) {
        todolistService.deleteTodolist(todoId);
    }

    // ✅ 여기 추가: 파일 검증 API
    /*@PostMapping("/{todoId}/validate-file")
    public boolean validateFileExtension(@PathVariable Long todoId,
                                         @RequestParam("file") MultipartFile file) {
        return todolistService.validateFileExtension(todoId, file);
    }*/

    @GetMapping("/team/{teamId}")
    public List<Todolist> getTodosByTeamId(@PathVariable Long teamId) {
        return todolistService.getTodosByTeamId(teamId);
    }

    // ✅ 여기 추가: 파일 검증 API
    @PostMapping("/{todoId}/verify")
    public String verifyFile(
            @PathVariable Long todoId,
            @RequestParam("file") MultipartFile file
    ) {
        return todolistService.verifyFile(todoId, file);
    }

    // ✅ 여기 추가: 파일 업로드 API
    /*@PostMapping("/{todoId}/submit")
    public String submitFile(
            @PathVariable Long todoId,
            @RequestParam("file") MultipartFile file
    ) {
        return todolistService.submitFile(todoId, file);
    }*/
    @PostMapping("/{todoId}/submit")
    public ResponseEntity<String> submitFile(
            @PathVariable Long todoId,
            @RequestParam("teammatesId") Long teammatesId,
            @RequestParam("file") MultipartFile file) {
        try {
            String result = todolistService.submitFile(todoId, teammatesId, file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("요청 오류: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 오류: " + e.getMessage());
        }
    }

    @GetMapping("/{todoId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long todoId) {
        Resource resource = todolistService.downloadFile(todoId);
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
        return todolistService.getTodosByCategory(teamId);
    }

    /** 2) 이번 주(일~토) 할 일 조회 */
    @GetMapping("/team/{teamId}/weekly")
    public List<TodoSummaryDto> getWeekly(@PathVariable Long teamId) {
        return todolistService.getWeeklyTodos(teamId);
    }

    /** 3)특정 카테고리 할 일 조회**/
    @GetMapping("team/{teamId}/category/{catId}")
    public ResponseEntity<List<TodoSummaryDto>> getByCategoryId(
            @PathVariable Long teamId,
            @PathVariable Long catId
    ) {
        return ResponseEntity.ok(
                todolistService.getTodosByCategoryId(teamId, catId)
        );
    }

    @GetMapping("/me/weekly")
    public List<TeamWeeklyTodoDto> getMyWeeklyTodosByUser(
            @RequestParam("userId") Long userId
    ) {
        return todolistService.getMyWeeklyTodosByUser(userId);
    }
}


