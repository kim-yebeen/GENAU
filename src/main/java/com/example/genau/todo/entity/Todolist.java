package com.example.genau.todo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.genau.user.domain.User;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "todolist")
public class Todolist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoId;

    private Long catId;
    private Long teamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Transient
    private Long assigneeId;
    @ManyToMany
    @JoinTable(
            name = "todolist_assignees",
            joinColumns = @JoinColumn(name = "todo_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> assignees = new ArrayList<>();

    @Column(nullable = false)
    private String todoTitle;

    private String todoDes;
    private Boolean todoChecked = false;
    private LocalDate dueDate;
    private LocalDateTime todoTime;

    @Column(name = "file_form", length = 50, nullable = true) // ✅ 추가
    private String fileForm; // 요구하는 파일 확장자 (예: pdf, docx)

    @Column(name = "uploaded_file_path")
    private String uploadedFilePath;

    // ✅ 추가: 변환 상태 및 결과 정보
    @Column(name = "convert_status")
    private String convertStatus;  // e.g., WAITING, SUCCESS, FAILED

    @Column(name = "converted_file_url", columnDefinition = "TEXT")
    private String convertedFileUrl;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "todolist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TodolistFile> files = new ArrayList<>();


    public Long getTodoId() { return todoId; }
    public void setTodoId(Long todoId) { this.todoId = todoId; }

    public Long getCatId() { return catId; }
    public void setCatId(Long catId) { this.catId = catId; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public List<User> getAssignees() {return assignees;}
    public void setAssignees(List<User> assignees) {this.assignees = assignees;}

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

    public String getUploadedFilePath() { return uploadedFilePath; }
    public void setUploadedFilePath(String uploadedFilePath) { this.uploadedFilePath = uploadedFilePath; }

    public String getConvertStatus() { return convertStatus; }
    public void setConvertStatus(String convertStatus) { this.convertStatus = convertStatus; }

    public String getConvertedFileUrl() { return convertedFileUrl; }
    public void setConvertedFileUrl(String convertedFileUrl) { this.convertedFileUrl = convertedFileUrl; }

    public LocalDateTime getConvertedAt() { return convertedAt; }
    public void setConvertedAt(LocalDateTime convertedAt) { this.convertedAt = convertedAt; }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public List<TodolistFile> getFiles() { return files; }
    public void setFiles(List<TodolistFile> files) { this.files = files; }
}


