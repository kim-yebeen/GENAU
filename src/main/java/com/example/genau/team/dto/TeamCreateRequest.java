package com.example.genau.team.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamCreateRequest {
    private Long userId;     // 요청하는 사용자 ID
    private String teamName; // 팀 이름
    private String teamDesc; // 팀 설명
    private String teamProfileImg; //팀 프로필 사진 url
}
