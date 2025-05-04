package com.example.genau.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class InviteCreateResponseDto {
    private String token;
    private LocalDateTime expiresAt;

    public InviteCreateResponseDto(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }
}
