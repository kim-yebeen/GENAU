package com.example.genau.category.repository;

import com.example.genau.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ✅ teamId로 모든 카테고리 조회
    List<Category> findByTeam_TeamId(Long teamId);
}
