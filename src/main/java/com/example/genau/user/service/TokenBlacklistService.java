package com.example.genau.user.service;

import com.example.genau.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";

    public void blacklistToken(String token) {
        try {
            Date expiration = jwtUtil.getExpirationDateFromToken(token);
            Date now = new Date();

            if (expiration.before(now)) {
                System.out.println("이미 만료된 토큰, 블랙리스트 등록 생략");
                return;
            }

            long remainingTime = expiration.getTime() - now.getTime();
            String key = BLACKLIST_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(remainingTime));
            System.out.println("토큰 블랙리스트 등록: " + token.substring(0, 20) + "...");

        } catch (Exception e) {
            System.err.println("토큰 블랙리스트 처리 중 오류: " + e.getMessage());
        }
    }

    // ✅ 수정된 메서드
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            String result = redisTemplate.opsForValue().get(key);
            boolean isBlacklisted = "blacklisted".equals(result); // ✅ 올바른 비교

            System.out.println("블랙리스트 확인 - 토큰: " + token.substring(0, 20) + "..., 결과: " + isBlacklisted);
            return isBlacklisted;

        } catch (Exception e) {
            System.err.println("블랙리스트 확인 중 오류: " + e.getMessage());
            return false; // 오류 시 false 반환 (접근 허용)
        }
    }
}
