package com.example.genau.notice.controller;

import com.example.genau.notice.dto.NotificationDto;
import com.example.genau.notice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 1) 내 모든 알림 조회 */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @RequestParam("userId") Long userId
    ) {
        List<NotificationDto> list = notificationService.getNotificationsByUser(userId);
        return ResponseEntity.ok(list);
    }

    /** 2) 개별 알림 읽음 처리 */
    @PatchMapping("/{noticeId}/read")
    public ResponseEntity<Map<String,Object>> markAsRead(
            @PathVariable Long noticeId
    ) {
        notificationService.markAsRead(noticeId);
        return ResponseEntity.ok(Map.of(
                "message",  "알림이 읽음 처리되었습니다.",
                "noticeId", noticeId
        ));
    }
}
