package com.example.genau.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class WeekTodoDto {
    private Long todoId;
    private Long catId;
    private String catName;
    private Long teamId;
    private Long assigneeId;
    private String todoTitle;
    private String todoDes;
    private Boolean todoChecked;
    private LocalDate dueDate;
}