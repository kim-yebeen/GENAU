package com.example.genau.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InviteValidateResponseDto {
    private boolean valid;
    private Long teamId;
    private String email;
}