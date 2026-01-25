package com.tarosuke777.hc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.tarosuke777.hc.handler.MessageHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	@Autowired
	private MessageHandler messageHandler;

	@Override
	public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {

		WebSocketHandlerRegistration registration =
				registry.addHandler(messageHandler, "/hc-websocket");
		registration.setAllowedOrigins("*");
	}

}
