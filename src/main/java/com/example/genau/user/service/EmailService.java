package com.example.genau.user.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
// …

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    // 인증 코드 저장/조회 prefix
    private static final String CODE_KEY_PREFIX     = "verify:";
    private static final String VERIFIED_KEY_PREFIX = "verified:";

    public void sendVerificationCode(String email)
            throws MessagingException, UnsupportedEncodingException {
        String code = String.format("%06d", new Random().nextInt(1_000_000));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(email);
        helper.setSubject("[GENAU] 이메일 인증 코드");

        InternetAddress from = new InternetAddress(
                "noreply@genau.com", "GENAU 팀", "UTF-8"
        );
        message.setFrom(from);

        String html = """
            <p>안녕하세요. <strong>GENAU</strong> 인증 번호 안내입니다.</p>
            <p>인증번호 : <strong>%s</strong></p>
            <p>인증번호는 <strong>3분간</strong> 유효합니다.</p>
            <br/>
            <p>감사합니다.</p>
            """.formatted(code);

        helper.setText(html, true);
        mailSender.send(message);

        // ★ 여기도 CODE_KEY_PREFIX 사용
        redisTemplate.opsForValue()
                .set(CODE_KEY_PREFIX + email, code, 3, TimeUnit.MINUTES);
    }

    public boolean verifyCode(String email, String code) {
        String key   = CODE_KEY_PREFIX + email;
        String saved = redisTemplate.opsForValue().get(key);

        if (saved != null && saved.equals(code)) {
            // 검증 플래그도 PREFIX 통일
            redisTemplate.opsForValue()
                    .set(VERIFIED_KEY_PREFIX + email, "true", 5, TimeUnit.MINUTES);
            return true;
        }
        return false;
    }

    public boolean isVerified(String email) {
        String flag = redisTemplate.opsForValue().get(VERIFIED_KEY_PREFIX + email);
        return "true".equals(flag);
    }
}
