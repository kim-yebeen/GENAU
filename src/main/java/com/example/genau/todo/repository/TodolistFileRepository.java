package com.example.genau.todo.repository;

import com.example.genau.todo.entity.TodolistFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TodolistFileRepository extends JpaRepository<TodolistFile, Long> {
    List<TodolistFile> findAllByTodolistTodoId(Long todoId);

    // 변환 상태별 조회
    List<TodolistFile> findAllByConvertStatus(String convertStatus);

    // 특정 Todo의 변환 상태별 파일 조회
    List<TodolistFile> findAllByTodolistTodoIdAndConvertStatus(Long todoId, String convertStatus);
}