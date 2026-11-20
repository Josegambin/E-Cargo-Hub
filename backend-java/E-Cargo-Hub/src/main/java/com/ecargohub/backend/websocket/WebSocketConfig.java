package com.ecargohub.backend.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.ecargohub.backend.websocket.StompJwtAuthenticationInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtAuthenticationInterceptor authenticationInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(StompJwtAuthenticationInterceptor authenticationInterceptor,
            @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200,http://localhost:8081,http://127.0.0.1:8081,http://localhost,http://127.0.0.1}") String[] allowedOrigins) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefijo de los topics a los que los clientes se suscriben
        config.enableSimpleBroker("/topic");

        // Prefijo de los mensajes que van a @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authenticationInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins).withSockJS();
        registry.addEndpoint("/ws-native").setAllowedOrigins(allowedOrigins);
    }
}