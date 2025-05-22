package com.example.genau.storage.controller;

import com.example.genau.storage.service.StorageService;
import com.example.genau.todo.entity.Todolist;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    // 제출 완료되고 마감일 기준 3일 후 확정된 파일 목록
    @GetMapping("/confirmed")
    public List<Todolist> getConfirmedFiles() {
        return storageService.getConfirmedFiles();
    }

    // 파일 삭제
    @DeleteMapping("/delete")
    public String deleteFile(@RequestParam("path") String filePath) {
        storageService.deleteFile(filePath);
        return "삭제 완료";
    }
}

