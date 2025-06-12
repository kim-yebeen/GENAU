package com.example.genau.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class InviteValidateResponseDto {
    private boolean valid;
    private Long teamId;
    private String email;
    private String teamName;    // 팀 이름 추가
    private String teamDesc;    // 팀 설명 추가

    // 기존 생성자 호환성을 위한 생성자
    public InviteValidateResponseDto(boolean valid, Long teamId, String email) {
        this.valid = valid;
        this.teamId = teamId;
        this.email = email;
    }
}