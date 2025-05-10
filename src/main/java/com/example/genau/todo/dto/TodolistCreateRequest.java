package com.example.genau.todo.dto;

import java.time.LocalDate;

public class TodolistCreateRequest {

    private Long teamId;

    private Long catId;
    private String todoTitle;
    private String todoDes;
    private LocalDate dueDate;
    private Long assigneeId;
    private Long creatorId;

    private String fileForm;
    // Getter & Setter
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public Long getCatId() {
        return catId;
    }
    public void setCatId(Long catId) {
        this.catId = catId;
    }

    public String getTodoTitle() { return todoTitle; }
    public void setTodoTitle(String todoTitle) { this.todoTitle = todoTitle; }

    public String getTodoDes() { return todoDes; }
    public void setTodoDes(String todoDes) { this.todoDes = todoDes; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Long getAssigneeId() { return assigneeId; } // ✅ 추가
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getFileForm() {
        return fileForm;
    }
    public void setFileForm(String fileForm) {
        this.fileForm = fileForm;
    }
}

