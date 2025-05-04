package com.example.genau.invitation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InviteAcceptRequestDto {
    private String token;
    private Long userId;
}
