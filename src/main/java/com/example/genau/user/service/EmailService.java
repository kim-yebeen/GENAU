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

    public void sendVerificationCode(String email) throws MessagingException, UnsupportedEncodingException {
        String code = String.format("%06d", new Random().nextInt(1_000_000));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(email);
        helper.setSubject("[GENAU] 이메일 인증 코드");

        // InternetAddress 생성 시 인코딩을 명시합니다.
        InternetAddress from = new InternetAddress(
                "noreply@genau.com",    // 발신 이메일
                "GENAU 팀",             // 표시 이름
                "UTF-8"                 // 인코딩
        );
        message.setFrom(from);

        // HTML 본문: 인증번호 부분을 <strong> 태그로 감싸 굵게 표시
        String html = """
            <p>안녕하세요. <strong>GENAU</strong> 인증 번호 안내입니다.</p>
            <p>인증번호 : <strong>%s</strong></p>
            <p>인증번호는 <strong>3분간</strong> 유효합니다.</p>
            <br/>
            <p>감사합니다.</p>
            """.formatted(code);

        // 두 번째 인자를 true로 주면 HTML 모드로 전송합니다.
        helper.setText(html, true);

        mailSender.send(message);

        redisTemplate.opsForValue()
                .set("verify:" + email, code, 3, TimeUnit.MINUTES);
    }
    public boolean verifyCode(String email, String code) {
        String saved = redisTemplate.opsForValue().get(email);
        boolean match = code.equals(saved);
        if (match) {
            // 인증 성공 표시를 위해 별도 키에 true 저장
            redisTemplate.opsForValue()
                    .set("verify:" + email, "true", 5, TimeUnit.MINUTES);
        }
        return match;
    }
}

