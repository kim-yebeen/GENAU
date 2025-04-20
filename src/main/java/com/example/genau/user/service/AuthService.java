package com.example.genau.user.service;

import com.example.genau.user.dto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.example.genau.user.domain.User;
import java.time.Duration;
import java.util.Random;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate redisTemplate;
    private final com.example.genau.user.repository.UserRepository userRepository;

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
        String verified = redisTemplate.opsForValue().get("verify:" + dto.getEmail());
        if (!"true".equals(verified)) {
            throw new RuntimeException("이메일 인증되지 않음");
        }

        // 유저 저장
        User user = User.builder()
                .userName(dto.getName())
                .userPw(dto.getPassword()) // 이건 나중에 암호화 필요
                .mail(dto.getEmail())
                .build();

        userRepository.save(user);

        System.out.println("회원가입 완료: " + dto.getEmail());
    }
}