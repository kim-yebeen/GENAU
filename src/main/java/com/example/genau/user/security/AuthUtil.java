package com.example.genau.user.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    /**
     * 현재 인증된 사용자의 ID를 가져옵니다.
     * JwtAuthFilter에서 SecurityContext에 저장한 userId를 반환합니다.
     *
     * @return 현재 사용자 ID
     * @throws RuntimeException 인증되지 않은 경우
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        }

        // JwtAuthFilter에서 principal에 userId를 저장했으므로
        Object principal = authentication.getPrincipal();

        if (principal instanceof Long) {
            return (Long) principal;
        }

        throw new RuntimeException("유효하지 않은 인증 정보입니다.");
    }

    /**
     * 현재 사용자가 인증되었는지 확인합니다.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal().equals("anonymousUser"));
    }
}