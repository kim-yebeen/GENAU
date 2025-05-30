package com.example.genau.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class TodoDetailDto {
    private final Long todoId;
    private final String todoTitle;
    private final String todoDes;
    private final LocalDate dueDate;
    private final Long assigneeId;
    private final Boolean todoChecked;
    private final String fileForm;
    private final String uploadedFilePath;
}
