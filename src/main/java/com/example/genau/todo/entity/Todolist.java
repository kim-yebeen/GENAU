package com.example.genau.todo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "todolist")
public class Todolist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoId;

    private Long catId;
    private Long teamId;
    private Long teammatesId;

    @Column(nullable = false)
    private String todoTitle;

    private String todoDes;
    private Boolean todoChecked = false;
    private LocalDate dueDate;
    private LocalDateTime todoTime;

    @Column(length = 50) // ✅ 추가
    private String fileForm; // 요구하는 파일 확장자 (예: pdf, docx)

    // ------------------
    // Getter & Setter
    // ------------------
    public Long getTodoId() { return todoId; }
    public void setTodoId(Long todoId) { this.todoId = todoId; }

    public Long getCatId() { return catId; }
    public void setCatId(Long catId) { this.catId = catId; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public Long getTeammatesId() { return teammatesId; }
    public void setTeammatesId(Long teammatesId) { this.teammatesId = teammatesId; }

    public String getTodoTitle() { return todoTitle; }
    public void setTodoTitle(String todoTitle) { this.todoTitle = todoTitle; }

    public String getTodoDes() { return todoDes; }
    public void setTodoDes(String todoDes) { this.todoDes = todoDes; }

    public Boolean getTodoChecked() { return todoChecked; }
    public void setTodoChecked(Boolean todoChecked) { this.todoChecked = todoChecked; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getTodoTime() { return todoTime; }
    public void setTodoTime(LocalDateTime todoTime) { this.todoTime = todoTime; }

    public String getFileForm() { return fileForm; } // ✅ 추가
    public void setFileForm(String fileForm) { this.fileForm = fileForm; } // ✅ 추가
}


