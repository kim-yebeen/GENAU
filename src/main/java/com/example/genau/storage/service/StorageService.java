package com.example.genau.storage.service;

import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.repository.TodolistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final TodolistRepository todolistRepository;

    public List<Todolist> getConfirmedFiles() {
        LocalDate today = LocalDate.now();
        return todolistRepository.findAll().stream()
                .filter(todo -> {
                    LocalDate due = todo.getDueDate();
                    return todo.getTodoChecked() != null && todo.getTodoChecked()
                            && due != null && today.isAfter(due.plusDays(3));
                })
                .toList();
    }

    public void deleteFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + e.getMessage());
        }
    }
}

