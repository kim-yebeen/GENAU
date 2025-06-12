package com.example.genau.storage.controller;

import com.example.genau.storage.service.StorageService;
import com.example.genau.user.security.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    /** 팀별 스토리지 조회 */
    @GetMapping("/{teamId}")
    public ResponseEntity<?> listTeamFiles(@PathVariable Long teamId) throws IOException {
        Long userId = AuthUtil.getCurrentUserId();
        List<String> files = storageService.listFilesByTeam(teamId, userId);

        if (files.isEmpty()) {
            return ResponseEntity.ok("❗ 현재 스토리지에 저장된 파일이 없습니다.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("teamId", teamId);
        response.put("fileCount", files.size());
        response.put("files", files);

        return ResponseEntity.ok(response);
    }

    /** 파일 다운로드 */
    @GetMapping("/{teamId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long teamId,
            @RequestParam("filename") String filename
    ) throws IOException {
        Long userId = AuthUtil.getCurrentUserId();
        Resource resource = storageService.downloadFile(teamId, filename, userId);
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                .body(resource);
    }

    @GetMapping("/team/{teamId}/todo/{todoId}")
    public ResponseEntity<Resource> downloadStoredFile(@PathVariable Long teamId, @PathVariable Long todoId) {
        Long userId = AuthUtil.getCurrentUserId();
        Resource resource = storageService.downloadStoredFile(teamId, todoId, userId);

        String encodedName = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
                .body(resource);
        /*
        try {
            Path folder = Paths.get(System.getProperty("user.dir"), "storage", "team-" + teamId);
            if (!Files.exists(folder)) {
                throw new IllegalArgumentException("해당 팀의 저장 폴더가 존재하지 않습니다.");
            }
            Path found = Files.list(folder)
                    .filter(p -> p.getFileName().toString().startsWith("todo-" + todoId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("해당 todo의 저장 파일이 없습니다."));

            Resource resource = new UrlResource(found.toUri());
            String encodedName = URLEncoder.encode(found.getFileName().toString(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
                    .body(resource);

        } catch (Exception e) {
            throw new RuntimeException("다운로드 실패: " + e.getMessage());
        }
        */
    }


    /** 파일 삭제 */
    @DeleteMapping("/{teamId}/delete")
    public ResponseEntity<String> deleteFile(
            @PathVariable Long teamId,
            @RequestParam("filename") String filename
    ) throws IOException {
        Long userId = AuthUtil.getCurrentUserId();
        storageService.deleteFile(teamId, filename, userId);
        return ResponseEntity.ok("삭제 완료: " + filename);
    }


}

