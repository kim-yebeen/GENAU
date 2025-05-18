package com.example.genau.notice.repository;

import com.example.genau.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByTeammatesId(Long teammatesId);
    // 내가 속한 모든 teammatesId 에 해당하는 알림을 최신순으로 가져온다
    List<Notice> findAllByTeammatesIdInOrderByNoticeCreatedDesc(List<Long> teammatesIds);
}