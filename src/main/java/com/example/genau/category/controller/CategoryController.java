package com.example.genau.category.controller;

import com.example.genau.category.dto.CategoryRequestDto;
import com.example.genau.category.dto.CategoryResponseDto;
import com.example.genau.category.dto.CategoryUpdateRequestDto;
import com.example.genau.category.service.CategoryService;
import com.example.genau.user.security.AuthUtil;
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
    public ResponseEntity<List<CategoryResponseDto>> getCategories(@PathVariable Long teamId) {
        Long userId = AuthUtil.getCurrentUserId();
        List<CategoryResponseDto> categories = categoryService.getCategoriesByTeamId(teamId, userId);
        return ResponseEntity.ok(categories);
    }

    // 1) 목록 등록
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCategory(
            @PathVariable Long teamId,
            @Valid @RequestBody CategoryRequestDto dto,
            BindingResult br
    ) {
        if (br.hasErrors()) {
            String err = br.getFieldError().getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("error", err));
        }
        Long userId = AuthUtil.getCurrentUserId();
        dto.setTeamId(teamId);
        Long catId = categoryService.createCategory(dto, userId);
        return ResponseEntity
                .status(201)
                .body(Map.of("message", "카테고리 생성 완료", "catId", catId));
    }

    // 2) 목록 수정
    @PutMapping("/{catId}")
    public ResponseEntity<Map<String,Object>> updateCategory(
            @PathVariable Long teamId,
            @PathVariable Long catId,
            @RequestBody CategoryUpdateRequestDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        categoryService.updateCategory(catId, dto, userId);
        return ResponseEntity.ok(Map.of("message", "카테고리명 수정 완료"));
    }

    // 3) 목록 삭제
    @DeleteMapping("/{catId}")
    public ResponseEntity<Map<String,Object>> deleteCategory(
            @PathVariable Long teamId,
            @PathVariable Long catId) {
        Long userId = AuthUtil.getCurrentUserId();
        categoryService.deleteCategory(catId, userId);
        return ResponseEntity.ok(Map.of("message", "카테고리 삭제 완료"));
    }

    // 4) 카테고리 색상 수정Add commentMore actions
    @PatchMapping("/{catId}/color")
    public ResponseEntity<Map<String, Object>> updateCategoryColor(
            @PathVariable Long teamId,
            @PathVariable Long catId,
            @RequestBody Map<String, String> request) {
        String catColor = request.get("catColor");
        if (catColor == null || catColor.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "색상 값이 필요합니다"));
        }

        Long userId = AuthUtil.getCurrentUserId();
        categoryService.updateCategoryColor(catId, catColor, userId);

        return ResponseEntity.ok(Map.of("message", "카테고리 색상 수정 완료"));
    }
}

