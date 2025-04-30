package com.example.genau.todo.dto;

import java.time.LocalDate;

public class TodolistCreateRequest {

    private Long teamId;
    private String todoTitle;
    private String todoDes;
    private LocalDate dueDate;

    // Getter & Setter
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getTodoTitle() { return todoTitle; }
    public void setTodoTitle(String todoTitle) { this.todoTitle = todoTitle; }

    public String getTodoDes() { return todoDes; }
    public void setTodoDes(String todoDes) { this.todoDes = todoDes; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}

