package com.example.genau.notice.controller;

import com.example.genau.notice.dto.NotificationDto;
import com.example.genau.notice.service.NotificationService;
import com.example.genau.user.security.AuthUtil;
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
    public ResponseEntity<List<NotificationDto>> getNotifications(){
        Long userId = AuthUtil.getCurrentUserId();
        List<NotificationDto> list = notificationService.getNotificationsByUser(userId);
        return ResponseEntity.ok(list);
    }

    //2) 알림 삭제 DELETE /notifications/{noticeId}
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long noticeId) {
        Long userId = AuthUtil.getCurrentUserId();
        notificationService.deleteNotification(noticeId, userId);
        return ResponseEntity.noContent().build();
    }

    // 3) 알림 읽음 표시 PUT /notifications/{noticeId}/read
    @PutMapping("/{noticeId}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long noticeId) {
        Long userId = AuthUtil.getCurrentUserId();
        notificationService.markAsRead(noticeId, userId);
        return ResponseEntity.ok().build();
    }

    // GET /notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        Long userId = AuthUtil.getCurrentUserId();
        long count = notificationService.getUnreadCountByUser(userId);
        return ResponseEntity.ok(count);
    }

}
