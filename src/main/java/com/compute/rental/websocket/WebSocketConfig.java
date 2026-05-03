package com.compute.rental.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ConsoleWebSocketHandler consoleWebSocketHandler;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            ConsoleWebSocketHandler consoleWebSocketHandler,
            @Value("${app.websocket.allowed-origins:*}") String[] allowedOrigins
    ) {
        this.consoleWebSocketHandler = consoleWebSocketHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(consoleWebSocketHandler, "/ws/console")
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
