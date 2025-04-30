package com.example.genau.todo.service;

import com.example.genau.todo.dto.TodolistCreateRequest;
import com.example.genau.todo.dto.TodolistUpdateRequest; // ✅ 추가
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.repository.TodolistRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional; // ✅ 추가

@Service
public class TodolistService {

    private final TodolistRepository todolistRepository;

    public TodolistService(TodolistRepository todolistRepository) {
        this.todolistRepository = todolistRepository;
    }

    public Todolist createTodolist(TodolistCreateRequest request) {
        Todolist todo = new Todolist();
        todo.setTeamId(request.getTeamId());
        todo.setTodoTitle(request.getTodoTitle());
        todo.setTodoDes(request.getTodoDes());
        todo.setDueDate(request.getDueDate());
        todo.setTodoTime(LocalDateTime.now());
        todo.setTodoChecked(false);

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

    public String submitFile(Long todoId, MultipartFile file) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        try {
            String originalFilename = file.getOriginalFilename();

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

            // (선택) DB에 저장
            // todo.setUploadedFilePath(filePath.toString());
            // todolistRepository.save(todo);

            return "파일 업로드 성공: " + filePath;
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage());
        }
    }


}




