package com.example.genau.category.repository;

import com.example.genau.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByTeam_TeamId(Long teamId);

    // ✅ 명시적 쿼리로 변경
    @Query("SELECT c FROM Category c WHERE c.team.teamId = :teamId ORDER BY c.catId ASC")
    List<Category> findByTeamId(@Param("teamId") Long teamId);
}
