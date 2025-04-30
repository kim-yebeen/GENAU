package com.example.genau.team.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teammates")
public class Teammates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SERIAL처럼 자동 증가
    private Long teammatesId;

    @Column(nullable = false)
    private Long userId; // 팀원(사용자) ID

    @Column(nullable = false)
    private Long teamId; // 팀 ID

    private LocalDateTime teamParticipated; // 팀 참여일

    @Column(nullable = false)
    private Boolean isManager; // ✅ 팀 관리 여부 (true = 팀장)

    public Long getTeammatesId() {
        return teammatesId;
    }

    public void setTeammatesId(Long teammatesId) {
        this.teammatesId = teammatesId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public LocalDateTime getTeamParticipated() {
        return teamParticipated;
    }

    public void setTeamParticipated(LocalDateTime teamParticipated) {
        this.teamParticipated = teamParticipated;
    }

    public Boolean getIsManager() {
        return isManager;
    }

    public void setIsManager(Boolean isManager) {
        this.isManager = isManager;
    }
}

