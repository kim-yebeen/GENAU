package com.example.genau.invitation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InviteCreateRequestDto {
    private Long teamId;    // 초대할 팀 ID
    private String email;   // 초대할 사용자 이메일
}
