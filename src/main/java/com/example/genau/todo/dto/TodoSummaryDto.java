package com.example.genau.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter @AllArgsConstructor
public class TodoSummaryDto {
    private Long todoId;
    private String todoTitle;
    private String todoDes;
    private LocalDate dueDate;
    private Boolean todoChecked;
    private Long   catId;
    private String categoryName;
}