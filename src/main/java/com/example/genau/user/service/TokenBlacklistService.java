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
                return;
            }

            // 남은 시간 계산
            long remainingTime = expiration.getTime() - now.getTime();

            // 토큰을 블랙리스트에 추가 (남은 시간만큼 TTL 설정)
            String key = BLACKLIST_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(remainingTime));

        } catch (Exception e) {
            // 토큰 파싱 실패 시에도 안전하게 처리
            // 기본 24시간으로 블랙리스트 등록
            String key = BLACKLIST_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofHours(24));
        }
    }

    //토큰이 블랙리스트에 있는지 확인

    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    /**
     * 특정 사용자의 모든 토큰을 무효화 (회원탈퇴 시 사용)
     * 실제로는 사용자별 토큰 추적이 필요하지만,
     * 현재 구조에서는 개별 토큰만 처리
     */
    public void blacklistUserTokens(Long userId) {
        // 현재 구조에서는 개별 토큰 추적이 어려우므로
        // 회원탈퇴 시에는 클라이언트에서 토큰 삭제에 의존
        // 필요시 사용자별 토큰 저장 로직을 별도로 구현해야 함
    }
}