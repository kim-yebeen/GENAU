package com.example.genau.category.service;

import com.example.genau.category.dto.CategoryRequestDto;
import com.example.genau.category.dto.CategoryUpdateRequestDto;
import com.example.genau.category.dto.CategoryResponseDto;
import com.example.genau.category.domain.Category;
import com.example.genau.category.repository.CategoryRepository;
import com.example.genau.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public Long createCategory(CategoryRequestDto dto) {
        var team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다."));
        Category cat = Category.builder()
                .team(team)
                .catName(dto.getCatName())
                .build();
        return categoryRepository.save(cat).getCatId();
    }

    @Transactional
    public void updateCategory(Long catId, CategoryUpdateRequestDto dto) {
        var cat = categoryRepository.findById(catId)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
        cat.setCatName(dto.getCatName());
    }

    @Transactional
    public void deleteCategory(Long catId) {
        categoryRepository.deleteById(catId);
    }

    // ✅ 목록 조회 메서드 추가
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategoriesByTeamId(Long teamId) {
        return categoryRepository.findByTeam_TeamId(teamId).stream()
                .map(CategoryResponseDto::from)
                .toList();
    }
}
