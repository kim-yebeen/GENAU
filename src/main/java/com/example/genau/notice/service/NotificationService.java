package com.example.genau.notice.service;

import com.example.genau.notice.dto.NotificationDto;
import com.example.genau.team.domain.Team;       // ← 추가
import com.example.genau.user.domain.User;       // ← 추가
import com.example.genau.notice.domain.Notice;
import com.example.genau.notice.repository.NoticeRepository;
import com.example.genau.team.domain.Teammates;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.repository.TodolistRepository;
import com.example.genau.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NoticeRepository         noticeRepository;
    private final TeammatesRepository      teammatesRepository;
    private final TeamRepository           teamRepository;
    private final TodolistRepository       todolistRepository;
    private final UserRepository           userRepository;

    /** 1) 팀 참여 즉시 알림 생성 */
    public void createTeamJoinNotification(Long newTeammatesId) {
        // 1) 새로 참여한 사람 정보
        Teammates newTm = teammatesRepository.findById(newTeammatesId)
                .orElseThrow();
        Long teamId = newTm.getTeamId();
        String newUserName = userRepository.findById(newTm.getUserId())
                .map(u -> u.getUserName())
                .orElse("알 수 없는 사용자");

        // 2) 같은 팀 기존 팀원들 (새로 들어온 사람은 제외)
        List<Teammates> existing = teammatesRepository.findAllByTeamId(teamId).stream()
                .filter(tm -> !tm.getTeammatesId().equals(newTeammatesId))
                .toList();

        // 3) 팀 이름
        String teamName = teamRepository.findById(teamId)
                .map(Team::getTeamName)
                .orElse("알 수 없는 팀");

        // 4) 알림 생성
        for (Teammates tm : existing) {
            Notice notice = Notice.builder()
                    .teammatesId(tm.getTeammatesId())
                    .noticeType("TEAM_JOIN")
                    .referenceId(teamId)                // 참조용: 팀 ID
                    .noticeMessage(
                            String.format("%s 팀에 %s 님이 참여했습니다", teamName, newUserName)
                    )
                    .build();
            noticeRepository.save(notice);
        }
    }

    /** 2) 할 일 완료 시 알림 생성 */
    public void createTodoCompleteNotification(Long todoId) {
        Todolist t = todolistRepository.findById(todoId)
                .orElseThrow();
        // 할 일 작성자(teammates_id)에게 알림
        Long creatorTmId = t.getCreatorId();

        Notice notice = Notice.builder()
                .teammatesId(creatorTmId)
                .noticeType("TODO_COMPLETE")
                .referenceId(todoId)
                .noticeMessage(
                        String.format("할 일 \"%s\" 이(가) 완료되었습니다.",
                                t.getTodoTitle())
                )
                .build();

        noticeRepository.save(notice);
    }

    /** 3) 매일 00:05AM 에 “내일 마감” 알림 일괄 생성 */
    @Scheduled(cron = "0 5 0 * * *")
    public void scheduleDueTomorrowNotifications() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Todolist> list =
                todolistRepository.findAllByDueDate(tomorrow);

        for (Todolist t : list) {
            Notice notice = Notice.builder()
                    .teammatesId(t.getAssigneeId())
                    .noticeType("DUE_TOMORROW")
                    .referenceId(t.getTodoId())
                    .noticeMessage(
                            String.format("할 일 \"%s\" 마감일이 내일입니다.",
                                    t.getTodoTitle())
                    )
                    .build();
            noticeRepository.save(notice);
        }
    }

    /** (1) 내 모든 알림 조회 */
    public List<NotificationDto> getNotificationsByUser (Long userId){
        // 1) userId 로 내 teammatesId 목록을 뽑아야 합니다
        List<Long> tmIds = teammatesRepository.findAllByUserId(userId)
                .stream()
                .map(Teammates::getTeammatesId)
                .toList();

        if (tmIds.isEmpty()) {
            return List.of();
        }

        // 2) 그 ID 들로 notice 조회 (OrderBy 생성일자 내림차순)
        List<Notice> notices = noticeRepository
                .findAllByTeammatesIdInOrderByNoticeCreatedDesc(tmIds);

        // 3) DTO 변환
        return notices.stream()
                .map(n -> new NotificationDto(
                        n.getNoticeId(),
                        n.getTeammatesId(),
                        n.getNoticeType(),
                        n.getReferenceId(),
                        n.getNoticeMessage(),
                        n.getNoticeCreated(),
                        n.getIsRead()
                ))
                .toList();
    }


    /** (2) 개별 알림 읽음 처리 */
    public void markAsRead(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("알림이 없습니다. id=" + noticeId));
        notice.setIsRead(true);
        noticeRepository.save(notice);
    }
}