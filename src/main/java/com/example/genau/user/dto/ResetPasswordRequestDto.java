// com.example.genau.user.dto.ResetPasswordRequestDto.java
package com.example.genau.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDto {
    private String email;
    private String newPassword;
    private String code;  // 이메일 인증코드
}

