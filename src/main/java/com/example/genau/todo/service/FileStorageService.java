package com.example.genau.todo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class FileStorageService {
    private final String uploadDir = System.getProperty("user.dir") + "/uploads";

    public String saveFile(MultipartFile file) {
        try {
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = dirPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + e.getMessage());
        }
    }
}
