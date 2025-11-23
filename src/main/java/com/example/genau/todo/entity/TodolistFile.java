package com.example.genau.todo.entity;

import com.example.genau.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TodolistFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String filePath;

    private String contentType;

    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id")
    private Todolist todolist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    private String convertStatus;  // WAITING, SUCCESS, FAILED, null
    private String convertedFilePath;  // 변환된 파일 저장 경로
    private LocalDateTime convertedAt;  // 변환 완료 시간

}
