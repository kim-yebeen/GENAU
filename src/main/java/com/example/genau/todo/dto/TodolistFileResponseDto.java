package com.example.genau.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodolistFileResponseDto {
    private Long id;
    private String fileName;
    private String filePath;
    private String contentType;
    private LocalDateTime uploadedAt;
    private String convertStatus;
    private String convertedFilePath;
    private LocalDateTime convertedAt;
    private Long uploaderId;
    private String uploaderName;
}