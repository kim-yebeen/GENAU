package com.example.genau.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationDto {
    private Long   noticeId;
    private Long   teammatesId;
    private String noticeType;
    private Long   referenceId;
    private String noticeMessage;
    private LocalDateTime noticeCreated;
    private Boolean isRead;
}
