package com.example.genau.todo.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TodoUpdateHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // ✅ 중복 세션 제거
        sessions.removeIf(s -> !s.isOpen());
        sessions.add(session);
        System.out.println("✅ 웹소켓 연결 성공: " + session.getId() + " (총 " + sessions.size() + "개 세션)");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("🔴 웹소켓 연결 종료: " + session.getId() + " (총 " + sessions.size() + "개 세션)");
    }

    public void broadcast(String message) {
        System.out.println("📤 브로드캐스트 메시지: " + message + " (대상: " + sessions.size() + "개 세션)");

        // ✅ 유효한 세션에만 메시지 전송
        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    return false; // 세션 유지
                } else {
                    return true; // 닫힌 세션 제거
                }
            } catch (IOException e) {
                System.err.println("메시지 전송 실패: " + e.getMessage());
                return true; // 오류 세션 제거
            }
        });
    }
}
