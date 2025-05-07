package com.example.genau.team.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TeamSummaryDto {
    private Long teamId;
    private String teamName;
    private String teamDesc;
    private String teamProfileImg;
    private Boolean isManager;
    private LocalDateTime joinedAt;  // teammates.teamParticipated
    private LocalDateTime updatedAt; // team.teamUpdated
}