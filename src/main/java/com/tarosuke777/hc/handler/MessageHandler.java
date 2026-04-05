package com.tarosuke777.hc.handler;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarosuke777.hc.entity.Message;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;

@Component
public class MessageHandler extends TextWebSocketHandler {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final ChatClient chatClient;
	private final DynamoDbTemplate dynamoDbTemplate;
	private final ConcurrentMap<String, Set<WebSocketSession>> channelSessionPool =
			new ConcurrentHashMap<>();

	public MessageHandler(ChatClient chatClient, DynamoDbTemplate dynamoDbTemplate) {
		this.chatClient = chatClient;
		this.dynamoDbTemplate = dynamoDbTemplate;
	}

	@Override
	public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
		String channelId = getChannelId(session);
		if (!StringUtils.hasText(channelId)) {
			return;
		}

		channelSessionPool.computeIfAbsent(channelId, key -> new CopyOnWriteArraySet<>()).add(session);
	}

	@Override
	protected void handleTextMessage(@NonNull WebSocketSession session,
			@NonNull TextMessage textMessage) {
		try {
			Message message = OBJECT_MAPPER.readValue(textMessage.getPayload(), Message.class);
			message.setCreatedAt(Instant.now().toString());
			message.setUserId("tarosuke777");

			saveMessage(message);
			sendMessage(message);

			if (StringUtils.hasText(message.getTo()) && StringUtils.hasText(message.getChannelId())) {
				handleAiMessage(message.getContent(), message.getChannelId());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void saveMessage(Message message) {
		dynamoDbTemplate.save(message);
	}

	public void sendMessage(Message message) throws JsonProcessingException, IOException {
		TextMessage sendTextMessage = new TextMessage(OBJECT_MAPPER.writeValueAsString(message));
		for (WebSocketSession webSocketSession : channelSessionPool.getOrDefault(message.getChannelId(), Collections.emptySet())) {
			webSocketSession.sendMessage(sendTextMessage);
		}
	}

	private void handleAiMessage(String content, String channelId) throws Exception {
		String response = chatClient.prompt().user(content).call().content();

		Message message = new Message();
		message.setContent(response);
		message.setChannelId(channelId);
		message.setCreatedAt(Instant.now().toString());
		message.setUserId("ollama");

		saveMessage(message);
		sendMessage(message);
	}

	@Override
	public void afterConnectionClosed(@NonNull WebSocketSession session,
			@NonNull CloseStatus status) throws Exception {
		String channelId = getChannelId(session);
		if (!StringUtils.hasText(channelId)) {
			return;
		}

		channelSessionPool.computeIfPresent(channelId, (key, sessions) -> {
			sessions.remove(session);
			return sessions.isEmpty() ? null : sessions;
		});
	}

	private String getChannelId(@NonNull WebSocketSession session) {
		URI uri = session.getUri();
		return (uri == null) ? null : uri.getQuery();
	}
}
