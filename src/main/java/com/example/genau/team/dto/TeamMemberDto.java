package com.example.genau.team.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TeamMemberDto {
    private Long userId;
    private String userName;
    private boolean isManager;
    private LocalDateTime participatedAt;
    private String profileImg;
}
