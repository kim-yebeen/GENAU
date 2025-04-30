package com.example.genau.team.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "team") // DB의 team 테이블과 매핑
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가
    private Long teamId;

    @Column(nullable = false)
    private Long userId; // 팀을 만든 사용자 ID

    @Column(nullable = false)
    private String teamName; // 팀 이름

    @Column(columnDefinition = "TEXT")
    private String teamDesc; // 팀 설명 (옵션)

    @Column(name = "team_profile_img") // ✅ 프로필 사진 필드 추가
    private String teamProfileImg;

    private LocalDateTime teamCreated; // 생성일
    private LocalDateTime teamUpdated; // 수정일

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamDesc() {
        return teamDesc;
    }

    public void setTeamDesc(String teamDesc) {
        this.teamDesc = teamDesc;
    }

    public String getTeamProfileImg() {
        return teamProfileImg;
    }

    public void setTeamProfileImg(String teamProfileImg) {
        this.teamProfileImg = teamProfileImg;
    }

    public LocalDateTime getTeamCreated() {
        return teamCreated;
    }

    public void setTeamCreated(LocalDateTime teamCreated) {
        this.teamCreated = teamCreated;
    }

    public LocalDateTime getTeamUpdated() {
        return teamUpdated;
    }

    public void setTeamUpdated(LocalDateTime teamUpdated) {
        this.teamUpdated = teamUpdated;
    }
}


