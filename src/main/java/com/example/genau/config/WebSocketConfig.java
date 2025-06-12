package com.example.genau.config;

import com.example.genau.notice.handler.NotificationUpdateHandler;
import com.example.genau.todo.handler.TodoUpdateHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TodoUpdateHandler todoUpdateHandler;
    private final NotificationUpdateHandler notificationUpdateHandler;

    public WebSocketConfig(TodoUpdateHandler todoUpdateHandler,
                           NotificationUpdateHandler notificationUpdateHandler) {
        this.todoUpdateHandler = todoUpdateHandler;
        this.notificationUpdateHandler = notificationUpdateHandler;
    }


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // ✅ 일단 SockJS 제거하고 단순하게 테스트
        registry.addHandler(todoUpdateHandler, "/ws/todos")
                .setAllowedOriginPatterns("*"); // CORS 허용
        registry.addHandler(notificationUpdateHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*");
    }
}