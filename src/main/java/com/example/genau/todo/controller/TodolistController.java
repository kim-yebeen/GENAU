package com.example.genau.todo.controller;

import com.example.genau.todo.dto.TodolistCreateRequest;
import com.example.genau.todo.dto.TodolistUpdateRequest; // ✅ 추가
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.service.TodolistService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/api/todos")
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
            @RequestParam("file") MultipartFile file) {
        try {
            String result = todolistService.submitFile(todoId, file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("요청 오류: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 오류: " + e.getMessage());
        }
    }



}


