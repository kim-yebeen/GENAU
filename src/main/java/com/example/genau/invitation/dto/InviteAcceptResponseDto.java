package com.example.genau.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InviteAcceptResponseDto {
    private Long teamId;
    private String teamName;
    private String teamDesc;
}
