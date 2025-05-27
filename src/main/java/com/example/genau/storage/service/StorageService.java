package com.example.genau.storage.service;

import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.repository.TodolistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final TodolistRepository todolistRepository;

    public List<String> listFilesByTeam(Long teamId) throws IOException {
        Path teamDir = Paths.get(System.getProperty("user.dir"), "storage", "team-" + teamId);
        if (!Files.exists(teamDir)) {
            System.out.println("❌ teamDir does not exist: " + teamDir.toAbsolutePath());
            return List.of();
        }

        try (var stream = Files.list(teamDir)) {
            List<String> fileNames = stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            System.out.println("📁 Found files for team " + teamId + ": " + fileNames);
            return fileNames;
        }
    }

    public Resource downloadFile(Long teamId, String filename) throws IOException {
        Path filePath = Paths.get(System.getProperty("user.dir"), "storage", "team-" + teamId, filename);
        if (!Files.exists(filePath)) {
            throw new NoSuchFileException("파일이 존재하지 않습니다: " + filename);
        }

        return new UrlResource(filePath.toUri());
    }

    public void deleteFile(Long teamId, String filename) throws IOException {
        Path filePath = Paths.get(System.getProperty("user.dir"), "storage", "team-" + teamId, filename);
        Files.deleteIfExists(filePath);
    }

    public void moveToStorageIfConfirmed(Long todoId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + todoId));

        LocalDate due = todo.getDueDate();
        if (todo.getTodoChecked() && due != null && LocalDate.now().isAfter(due.plusDays(2))) {
            String uploadedPath = todo.getUploadedFilePath();
            if (uploadedPath == null || uploadedPath.isBlank()) return;

            Path source = Paths.get(uploadedPath);
            if (!Files.exists(source)) {
                System.out.println("⚠️ 업로드된 파일이 존재하지 않습니다: " + uploadedPath);
                return;
            }
            String extension = uploadedPath.substring(uploadedPath.lastIndexOf('.') + 1);
            Path storageRoot = Paths.get(System.getProperty("user.dir"), "storage"); // 🔍 storage 폴더 경로
            Path teamFolder = storageRoot.resolve("team-" + todo.getTeamId()); // 🔍 team-x 폴더
            Path target = teamFolder.resolve("todo-" + todo.getTodoId() + "." + extension); // 저장 경로

            try {
                Files.createDirectories(teamFolder);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ 스토리지로 파일 복사 완료: " + target);
            } catch (IOException e) {
                throw new RuntimeException("스토리지 이동 실패: " + e.getMessage());
            }
        }else {
            System.out.println("ℹ️ 조건 미충족: todoChecked=" + todo.getTodoChecked() +
                    ", dueDate=" + due + ", today=" + LocalDate.now());
        }
    }

}


