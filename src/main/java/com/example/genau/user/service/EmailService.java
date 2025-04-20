package com.example.genau.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final StringRedisTemplate redisTemplate;

    public String sendVerificationCode(String email) {
        String code = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set(email, code, 3, TimeUnit.MINUTES);
        System.out.println("[인증코드] " + email + " -> " + code); // 실제로는 이메일 발송
        return code;
    }

    public boolean verifyCode(String email, String code) {
        String saved = redisTemplate.opsForValue().get(email);
        boolean match = code.equals(saved);

        if (match) {
            // 인증된 이메일 표시 키 저장
            redisTemplate.opsForValue().set("verify:" + email, "true", 5, TimeUnit.MINUTES);
        }

        return match;
    }

}
