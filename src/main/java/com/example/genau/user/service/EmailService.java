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


    /**
     * 팀 초대 링크를 HTML 메일로 발송합니다.
     * @param toEmail  초대 받을 사람 이메일
     * @param link     초대 수락용 URL
     */
    public void sendInvitationLink(String toEmail, String link) {
        // 메일 제목
        String subject = "[GENAU] 팀 스페이스 초대";

        // HTML 본문: 링크 클릭 유도 + 만료 안내
        String htmlBody = """
            <p>안녕하세요!</p>
            <p>다음 링크를 클릭하시면 <strong>GENAU</strong> 팀 스페이스에 참여하실 수 있습니다:</p>
            <p><a href="%s" target="_blank">팀 스페이스 참가하기</a></p>
            <p>• 이 링크는 발행 후 <strong>7일간</strong> 유효하며,<br/>
            • 한 번만 사용 가능합니다.</p>
            <br/>
            <p>감사합니다.</p>
            """.formatted(link);

        // 실제 전송
        sendHtmlMail(toEmail, subject, htmlBody);
    }

    //HTML 형식 메일 보내기
    public void sendHtmlMail(String to, String subject, String htmlBody) {
        MimeMessage msg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);  // true = HTML 모드

            // 발신자 표시(원하시면 수정)
            InternetAddress from = new InternetAddress(
                    "noreply@genau.com", "GENAU 팀", "UTF-8"
            );
            msg.setFrom(from);

            mailSender.send(msg);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("초대 메일 발송에 실패했습니다.", e);
        }
    }
}
