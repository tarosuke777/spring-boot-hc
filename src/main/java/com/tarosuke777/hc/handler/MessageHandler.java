package com.tarosuke777.hc.handler;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.tarosuke777.hc.dto.MessageRequest;
import com.tarosuke777.hc.dto.MessageResponse;
import com.tarosuke777.hc.entity.Message;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;

@Component
public class MessageHandler extends TextWebSocketHandler {

	private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String WS_USER_ID = "tarosuke777";
	private static final String AI_USER_ID = "ollama";

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

		channelSessionPool.computeIfAbsent(channelId, key -> new CopyOnWriteArraySet<>())
				.add(session);
	}

	@Override
	protected void handleTextMessage(@NonNull WebSocketSession session,
			@NonNull TextMessage textMessage) {
		try {
			MessageRequest request =
					OBJECT_MAPPER.readValue(textMessage.getPayload(), MessageRequest.class);
			Message message = buildMessageFromRequest(request, WS_USER_ID);
			MessageResponse response = toMessageResponse(message);

			saveMessage(message);
			sendMessage(response);

			if (StringUtils.hasText(request.getTo())
					&& StringUtils.hasText(request.getChannelId())) {
				handleAiMessage(response.getContent(), response.getChannelId());
			}
		} catch (IOException e) {
			logger.error("Failed to parse WebSocket message", e);
		} catch (Exception e) {
			logger.error("Error handling WebSocket message", e);
		}
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

	public void sendMessage(MessageResponse messageResponse)
			throws JsonProcessingException, IOException {
		TextMessage sendTextMessage =
				new TextMessage(OBJECT_MAPPER.writeValueAsString(messageResponse));
		for (WebSocketSession webSocketSession : channelSessionPool
				.getOrDefault(messageResponse.getChannelId(), Collections.emptySet())) {
			webSocketSession.sendMessage(sendTextMessage);
		}
	}

	private void handleAiMessage(String content, String channelId) throws Exception {
		String response = chatClient.prompt().user(content).call().content();

		Message message = new Message();
		message.setContent(response);
		message.setChannelId(channelId);
		message.setCreatedAt(Instant.now().toString());
		message.setUserId(AI_USER_ID);

		saveMessage(message);
		sendMessage(toMessageResponse(message));
	}

	private Message buildMessageFromRequest(MessageRequest request, String userId) {
		Message message = new Message();
		message.setChannelId(
				StringUtils.hasText(request.getChannelId()) ? request.getChannelId() : "1");
		message.setContent(StringUtils.hasText(request.getContent()) ? request.getContent() : "");
		message.setCreatedAt(Instant.now().toString());
		message.setUserId(userId);
		return message;
	}

	private MessageResponse toMessageResponse(Message message) {
		MessageResponse response = new MessageResponse();
		response.setChannelId(message.getChannelId());
		response.setCreatedAt(message.getCreatedAt());
		response.setContent(message.getContent());
		response.setUserId(message.getUserId());
		return response;
	}

	private String getChannelId(@NonNull WebSocketSession session) {
		URI uri = session.getUri();
		return (uri == null) ? null : uri.getQuery();
	}

	private void saveMessage(Message message) {
		dynamoDbTemplate.save(message);
	}
}
