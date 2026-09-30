package com.ecargohub.backend.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefijo de los topics a los que los clientes se suscriben
        config.enableSimpleBroker("/topic");

        // Prefijo de los mensajes que van a @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de conexión WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:4200", "http://localhost", "http://localhost:*",
                        "http://127.0.0.1:*") // en producción, restringe a tu frontend
                .withSockJS();

        // 🌟 NUEVO ENDPOINT DIRECTO: Para clientes nativos offline sin librerías externas
        registry.addEndpoint("/ws-native").setAllowedOriginPatterns("http://localhost:4200", "http://localhost",
                "http://localhost:*", "http://127.0.0.1:*"); // Sin .withSockJS() al final
    }
}