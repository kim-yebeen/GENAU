package com.example.genau.storage.service;

import com.example.genau.team.domain.Team;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
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
import org.springframework.security.access.AccessDeniedException;
@Service
@RequiredArgsConstructor
public class StorageService {

    private final TodolistRepository todolistRepository;
    private final TeamRepository teamRepository;
    private final TeammatesRepository teammatesRepository;


    public List<String> listFilesByTeam(Long teamId, Long userId) throws IOException {
        validateTeamMembership(teamId, userId);

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

    public Resource downloadFile(Long teamId, String filename, Long userId) throws IOException {
        validateTeamMembership(teamId, userId);

        Path filePath = Paths.get(System.getProperty("user.dir"), "storage", "team-" + teamId, filename);
        if (!Files.exists(filePath)) {
            throw new NoSuchFileException("파일이 존재하지 않습니다: " + filename);
        }

        return new UrlResource(filePath.toUri());
    }

    // ✅ TODO 관련 파일 다운로드 - Auth 적용 (팀원만 다운로드 가능)
    public Resource downloadStoredFile(Long teamId, Long todoId, Long userId) {
        // 팀원인지 확인
        validateTeamMembership(teamId, userId);

        try {
            Path folder = Paths.get(System.getProperty("user.dir"), "storage", "team-" + teamId);
            if (!Files.exists(folder)) {
                throw new IllegalArgumentException("해당 팀의 저장 폴더가 존재하지 않습니다.");
            }

            Path found = Files.list(folder)
                    .filter(p -> p.getFileName().toString().startsWith("todo-" + todoId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("해당 todo의 저장 파일이 없습니다."));

            return new UrlResource(found.toUri());
        } catch (Exception e) {
            throw new RuntimeException("다운로드 실패: " + e.getMessage());
        }
    }
    public void deleteFile(Long teamId, String filename, Long userId) throws IOException {
        validateTeamMembership(teamId, userId);
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

    private void validateTeamMembership(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("해당 팀이 없습니다."));

        boolean isMember = team.getUserId().equals(userId)
                || teammatesRepository.existsByTeamIdAndUserId(teamId, userId);

        if (!isMember) {
            throw new AccessDeniedException("팀원만 스토리지에 접근할 수 있습니다.");
        }
    }
}


