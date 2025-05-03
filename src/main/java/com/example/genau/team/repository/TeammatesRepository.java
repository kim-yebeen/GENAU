package com.example.genau.team.repository;

import com.example.genau.team.domain.Teammates;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeammatesRepository extends JpaRepository<Teammates, Long> {
    // 팀 나가기용
    void deleteByTeamIdAndUserId(Long teamId, Long userId);
    // 팀 삭제 시 전체 팀원 삭제용
    void deleteAllByTeamId(Long teamId);

    boolean existsByTeamIdAndUserIdAndIsManagerTrue(Long teamId, Long userId);
}

