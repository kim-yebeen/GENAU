package com.example.genau.user.security;

import com.example.genau.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization 헤더에서 JWT 토큰 추출
        String authHeader = request.getHeader("Authorization");

        // 2. 토큰이 없거나 Bearer 형식이 아니면 다음 필터로 진행
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 3. 토큰 유효성 검증
            if (jwtUtil.validateToken(token)) {
                // 4. 토큰이 유효하면 사용자 ID 추출
                Long userId = jwtUtil.getUserId(token);

                if (!userRepository.existsById(userId)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"유효하지 않은 사용자입니다.\"}");
                    return;  // 필터 체인 진행 중단
                }

                // 5. Spring Security 인증 객체 생성 및 설정
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

                // 6. SecurityContext에 인증 객체 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 7. 토큰 처리 중 예외 발생 시 인증 실패 처리
            SecurityContextHolder.clearContext();
        }

        // 8. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}