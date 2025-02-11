package com.tarosuke777.hc.handler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarosuke777.hc.entity.Message;

import io.awspring.cloud.dynamodb.DynamoDbTemplate;

public class Handler extends TextWebSocketHandler {

	private ConcurrentHashMap<String, Set<WebSocketSession>> channelSessionPool = new ConcurrentHashMap<>();

	@Autowired
	DynamoDbTemplate dynamoDbTemplate;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {

		String channelId = session.getUri().getQuery();

		channelSessionPool.compute(channelId, (key, sessions) -> {

			if (sessions == null) {
				sessions = new CopyOnWriteArraySet<>();
			}
			sessions.add(session);

			return sessions;
		});
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {

		ObjectMapper mapper = new ObjectMapper();
		try {
			Message message = mapper.readValue(textMessage.getPayload(), Message.class);
			message.setChannelId(message.getChannelId());
			message.setCreatedAt(LocalDateTime.now().toString());
			message.setUserId("1");

			dynamoDbTemplate.save(message);

			TextMessage sendTextMessage = new TextMessage(mapper.writeValueAsString(message));

			for (WebSocketSession webSocketSession : channelSessionPool.get(message.getChannelId())) {
				webSocketSession.sendMessage(sendTextMessage);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

		String channelId = session.getUri().getQuery();

		channelSessionPool.compute(channelId, (key, sessions) -> {

			sessions.remove(session);
			if (sessions.isEmpty()) {
				sessions = null;
			}

			return sessions;
		});
	}
}