package com.example.genau.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter @AllArgsConstructor
public class CategoryTodoDto {
    private Long catId;
    private String categoryName;
    private List<TodoSummaryDto> todos;
}