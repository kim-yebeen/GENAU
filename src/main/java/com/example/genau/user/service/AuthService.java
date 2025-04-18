package com.example.genau.user.service;

import com.example.genau.user.dto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisTemplate<String, String> redisTemplate;

    public void sendEmailVerificationCode(String email) {
        String code = String.valueOf(new Random().nextInt(899999) + 100000);
        redisTemplate.opsForValue().set("verify:" + email, code, Duration.ofMinutes(5));
        // 실제 메일 전송 로직 생략
        System.out.println("이메일 인증코드: " + code);
    }

    public boolean verifyEmailCode(String email, String code) {
        String key = "verify:" + email;
        String savedCode = redisTemplate.opsForValue().get(key);
        return savedCode != null && savedCode.equals(code);
    }

    public void signup(SignupRequestDto dto) {
        String verifiedCode = redisTemplate.opsForValue().get("verify:" + dto.getEmail());
        if (verifiedCode == null) throw new RuntimeException("이메일 인증되지 않음");
        // DB 저장 로직 생략
        System.out.println("회원가입 완료: " + dto.getEmail());
    }
}