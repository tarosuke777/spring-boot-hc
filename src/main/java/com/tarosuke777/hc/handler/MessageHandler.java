package com.tarosuke777.hc.handler;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;

@Component
public class MessageHandler extends TextWebSocketHandler {

	@Value("${ai.model.host}")
	private String modelHost;

	@Value("${ai.sse.host}")
	private String sseHost;

	interface Assistant {
		String chat(String userMessage);
	}

	private ConcurrentHashMap<String, Set<WebSocketSession>> channelSessionPool =
			new ConcurrentHashMap<>();

	@Autowired
	DynamoDbTemplate dynamoDbTemplate;

	@Override
	public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {

		String channelId = getChannelId(session);

		channelSessionPool.compute(channelId, (key, sessions) -> {

			if (sessions == null) {
				sessions = new CopyOnWriteArraySet<>();
			}
			sessions.add(session);

			return sessions;
		});
	}

	@Override
	protected void handleTextMessage(@NonNull WebSocketSession session,
			@NonNull TextMessage textMessage) {

		ObjectMapper mapper = new ObjectMapper();
		Instant nowUtc = Instant.now();

		try {
			Message message = mapper.readValue(textMessage.getPayload(), Message.class);
			message.setChannelId(message.getChannelId());
			message.setCreatedAt(nowUtc.toString());
			message.setUserId("tarosuke777");

			dynamoDbTemplate.save(message);

			sendMessage(mapper, message);

			System.out.println(message);

			if (StringUtils.hasText(message.getTo())) {
				try {
					handleAiMessage(message.getContent(), message.getChannelId(), message.getTo());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void sendMessage(ObjectMapper mapper, Message message)
			throws JsonProcessingException, IOException {
		TextMessage sendTextMessage = new TextMessage(mapper.writeValueAsString(message));

		for (WebSocketSession webSocketSession : channelSessionPool.get(message.getChannelId())) {
			webSocketSession.sendMessage(sendTextMessage);
		}
	}

	private void handleAiMessage(String content, String channelId, String to) throws Exception {

		String modelName = "qwen3:4b";

		ChatModel model = OllamaChatModel.builder().baseUrl(modelHost).modelName(modelName)
				.timeout(Duration.ofSeconds(360)).responseFormat(ResponseFormat.TEXT).build();
		String res = model.chat(content);
		System.out.println(res);

		Instant nowUtc = Instant.now();

		Message message = new Message();
		message.setContent(res);
		message.setChannelId(channelId);
		message.setCreatedAt(nowUtc.toString());
		message.setUserId(modelName);
		dynamoDbTemplate.save(message);

		ObjectMapper mapper = new ObjectMapper();
		sendMessage(mapper, message);

	}

	@Override
	public void afterConnectionClosed(@NonNull WebSocketSession session,
			@NonNull CloseStatus status) throws Exception {

		String channelId = getChannelId(session);

		channelSessionPool.compute(channelId, (key, sessions) -> {

			sessions.remove(session);
			if (sessions.isEmpty()) {
				sessions = null;
			}

			return sessions;
		});
	}

	private String getChannelId(@NonNull WebSocketSession session) {
		URI uri = session.getUri();
		if (uri == null || uri.getQuery() == null) {
			return null;
		}
		return uri.getQuery();
	}

}
