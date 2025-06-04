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

    //토큰을 블랙리스트에 추가토큰의 남은 만료시간만큼 Redis에 저장
    public void blacklistToken(String token) {
        try {
            // 토큰에서 만료시간 추출
            Date expiration = jwtUtil.getExpirationDateFromToken(token);
            Date now = new Date();

            // 토큰이 이미 만료되었다면 블랙리스트에 추가할 필요 없음
            if (expiration.before(now)) {
                System.out.println("이미 만료된 토큰, 블랙리스트 등록 생략");
                return;
            }

            // 남은 시간 계산
            long remainingTime = expiration.getTime() - now.getTime();

            // 토큰을 블랙리스트에 추가 (남은 시간만큼 TTL 설정)
            String key = BLACKLIST_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(remainingTime));
            System.out.println("토큰 블랙리스트 등록: " + token.substring(0, 20) + "...");

        } catch (Exception e) {
            System.err.println("토큰 블랙리스트 처리 중 오류: " + e.getMessage());
            // ❌ 예외 시 자동 등록하지 않음
        }

    }

    //토큰이 블랙리스트에 있는지 확인
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String key = BLACKLIST_KEY_PREFIX + token;
        String result = redisTemplate.opsForValue().get(key);
        return redisTemplate.equals(result);
    }
}