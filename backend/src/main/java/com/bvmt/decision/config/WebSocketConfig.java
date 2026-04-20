package com.bvmt.decision.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket + STOMP pour la diffusion temps réel des alertes
 * et signaux de trading vers le frontend Angular.
 *
 * Endpoints :
 *   ws://host/api/ws           ← endpoint STOMP
 *   /topic/signals             ← signaux de trading (broadcast)
 *   /topic/alerts              ← alertes RSI / seuils (broadcast)
 *   /topic/prices/{ticker}     ← prix par instrument (broadcast spécifique)
 *   /user/queue/notifications  ← notifications personnelles (point-à-point)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker en mémoire (suffit pour un MVP ; passer à RabbitMQ/ActiveMQ en prod HA)
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")   // à restreindre en prod via CORS
                .withSockJS();                    // fallback SockJS pour navigateurs restrictifs
    }
}
