package com.example.genau.invitation.repository;

import com.example.genau.invitation.domain.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByToken(String token);

    // 특정 이메일과 팀의 미수락 초대 존재 여부 확인
    boolean existsByEmailAndTeamIdAndAcceptedFalse(String email, Long teamId);

    // 만료된 초대 조회
    @Query("SELECT i FROM Invitation i WHERE i.expiresAt < :now AND i.accepted = false")
    List<Invitation> findExpiredInvitations(@Param("now") LocalDateTime now);

    // 특정 팀의 모든 초대 조회
    List<Invitation> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    // 특정 이메일의 모든 초대 조회
    List<Invitation> findByEmailOrderByCreatedAtDesc(String email);
}