package com.example.genau.category.service;

import com.example.genau.category.dto.CategoryRequestDto;
import com.example.genau.category.dto.CategoryUpdateRequestDto;
import com.example.genau.category.dto.CategoryResponseDto;
import com.example.genau.category.domain.Category;
import com.example.genau.category.repository.CategoryRepository;
import com.example.genau.team.domain.Team;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TeamRepository teamRepository;
    private final TeammatesRepository teammatesRepository;

    @Transactional
    public Long createCategory(CategoryRequestDto dto, Long userId) {

        validateTeamMembership(dto.getTeamId(), userId);

        Team team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다."));

        Category cat = Category.builder()
                .team(team)
                .catName(dto.getCatName())
                .build();
        Category saveCategory = categoryRepository.save(cat);
        return saveCategory.getCatId();
    }

    @Transactional
    public void updateCategory(Long catId, CategoryUpdateRequestDto dto, Long userId) {

        Category cat = categoryRepository.findById(catId)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));

        validateTeamMembership(cat.getTeam().getTeamId(), userId);

        cat.setCatName(dto.getCatName());
        categoryRepository.save(cat);
    }

    @Transactional
    public void deleteCategory(Long catId, Long userId)
    {
        Category cat = categoryRepository.findById(catId)
                        .orElseThrow(() -> new IllegalArgumentException("해당 카테고리가 없습니다."));

        validateTeamMembership(cat.getTeam().getTeamId(), userId);

        categoryRepository.deleteById(catId);
    }

    // 팀원인지 확인하는 공통 메서드
    private void validateTeamMembership(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("해당 팀이 없습니다."));

        boolean isMember = team.getUserId().equals(userId)
                || teammatesRepository.existsByTeamIdAndUserId(teamId, userId);

        if (!isMember) {
            throw new AccessDeniedException("팀원만 카테고리를 관리할 수 있습니다.");
        }
    }


    // 목록 조회 메서드 추가
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategoriesByTeamId(Long teamId, Long userId) {
        validateTeamMembership(teamId, userId);

        List<Category> categories = categoryRepository.findByTeam_TeamId(teamId);
        return categories.stream()
                .map(cat -> new CategoryResponseDto(cat.getCatId(), cat.getCatName()))
                .collect(Collectors.toList());
    }
}
