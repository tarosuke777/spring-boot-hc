package com.tarosuke777.hc.handler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarosuke777.hc.entity.Message;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;

public class Handler extends TextWebSocketHandler {

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

			TextMessage sendTextMessage = new TextMessage(mapper.writeValueAsString(message));

			for (WebSocketSession webSocketSession : channelSessionPool
					.get(message.getChannelId())) {
				webSocketSession.sendMessage(sendTextMessage);
			}

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

	private void handleAiMessage(String content, String channelId, String to) throws Exception {

		String sseUrl = sseHost + "/hms/sse";

		McpTransport transport = new HttpMcpTransport.Builder().sseUrl(sseUrl).build();
		McpClient client = new DefaultMcpClient.Builder().transport(transport).build();
		ToolProvider provider = McpToolProvider.builder().mcpClients(client).build();

		String modelName = to;
		String modelUrl = modelHost + "/engines/llama.cpp/v1";

		ChatModel model =
				OpenAiChatModel.builder().baseUrl(modelUrl).modelName(modelName)
						.httpClientBuilder(JdkHttpClient.builder().httpClientBuilder(
								HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)))
						.build();

		Assistant assistant =
				AiServices.builder(Assistant.class).chatModel(model).toolProvider(provider).build();

		String res = assistant.chat("""
				/no_think
				""" + content);

		Instant nowUtc = Instant.now();

		// System.out.println(res);
		Message message = new Message();
		message.setContent(res);
		message.setChannelId(channelId);
		message.setCreatedAt(nowUtc.toString());
		message.setUserId(modelName);
		dynamoDbTemplate.save(message);

		ObjectMapper mapper = new ObjectMapper();
		TextMessage sendTextMessage = new TextMessage(mapper.writeValueAsString(message));

		for (WebSocketSession webSocketSession : channelSessionPool.get(message.getChannelId())) {
			webSocketSession.sendMessage(sendTextMessage);
		}

		client.close();

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
