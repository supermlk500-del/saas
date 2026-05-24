package com.zhihuitong.modules.quality.config;

import com.zhihuitong.modules.quality.websocket.QcStreamWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@EnableScheduling
@Configuration
public class QcStreamWebSocketConfig implements WebSocketConfigurer {

    private final QcStreamWebSocketHandler qcStreamWebSocketHandler;

    public QcStreamWebSocketConfig(QcStreamWebSocketHandler qcStreamWebSocketHandler) {
        this.qcStreamWebSocketHandler = qcStreamWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(qcStreamWebSocketHandler, "/ws/qc-stream/*")
                .setAllowedOriginPatterns("*");
    }
}
