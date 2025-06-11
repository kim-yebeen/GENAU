package com.example.genau.todo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class TodolistUpdateRequest {

    private String todoTitle;
    private String todoDes;
    private String fileForm;
    private Long assigneeId;

    // getter, setter 확인
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    // Getter & Setter
    public String getTodoTitle() {
        return todoTitle;
    }

    public void setTodoTitle(String todoTitle) {
        this.todoTitle = todoTitle;
    }

    public String getTodoDes() {
        return todoDes;
    }

    public void setTodoDes(String todoDes) {
        this.todoDes = todoDes;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getFileForm() {
        return fileForm;
    }

    public void setFileForm(String fileForm) {
        this.fileForm = fileForm;
    }
}