package com.example.genau.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class TodoCalendarSummaryDto {
    private Long teamId;
    private Long todoId;
    private String todoTitle;
    private LocalDate dueDate;
    private Boolean isMyTask; // 항상 true
}