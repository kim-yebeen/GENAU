package com.example.genau.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter @AllArgsConstructor
public class TodoSummaryDto {
    private Long todoId;
    private String todoTitle;
    private String todoDes;
    private LocalDate dueDate;
    private Boolean todoChecked;
    private String fileForm;
    private String uploadedFilePath;
    private Long   catId;
    private String categoryName;
    private List<Long> assignees;
    private List<String> assigneeNames;
    private Long creatorId;
}