package com.example.genau.category.controller;

import com.example.genau.category.dto.CategoryRequestDto;
import com.example.genau.category.dto.CategoryUpdateRequestDto;
import com.example.genau.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teams/{teamId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ✅ 0) 목록 조회
    @GetMapping
    public ResponseEntity<?> getCategories(@PathVariable Long teamId) {
        List<?> categories = categoryService.getCategoriesByTeamId(teamId);
        return ResponseEntity.ok(categories);
    }

    // 1) 목록 등록
    @PostMapping
    public ResponseEntity<?> createCategory(
            @PathVariable Long teamId,
            @Valid @RequestBody CategoryRequestDto dto,
            BindingResult br
    ) {
        if (br.hasErrors()) {
            String err = br.getFieldError().getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("error", err));
        }
        dto.setTeamId(teamId);
        Long catId = categoryService.createCategory(dto);
        return ResponseEntity
                .status(201)
                .body(Map.of("message", "카테고리 생성 완료", "catId", catId));
    }

    // 2) 목록 수정
    @PutMapping("/{catId}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long catId,
            @RequestBody CategoryUpdateRequestDto dto) {
        categoryService.updateCategory(catId, dto);
        return ResponseEntity.ok(Map.of("message", "카테고리명 수정 완료"));
    }

    // 3) 목록 삭제
    @DeleteMapping("/{catId}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long catId) {
        categoryService.deleteCategory(catId);
        return ResponseEntity.ok(Map.of("message", "카테고리 삭제 완료"));
    }
}
