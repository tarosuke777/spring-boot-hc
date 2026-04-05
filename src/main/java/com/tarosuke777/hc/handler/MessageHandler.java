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
import com.tarosuke777.hc.mapper.MessageMapper;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;

/**
 * WebSocket メッセージを受信して DynamoDB に保存し、接続中のクライアントにブロードキャストするハンドラー。
 * <p>
 * 受信したメッセージを内部エンティティに変換し、必要に応じて AI レスポンスを生成して 同じチャネルに送信します。
 */
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

	/**
	 * WebSocket 接続が確立されたときに呼び出される。 チャネル ID ごとにセッションをプールに追加する。
	 *
	 * @param session 新しい WebSocket セッション
	 * @throws Exception セッションハンドリングに失敗した場合
	 */
	@Override
	public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
		String channelId = getChannelId(session);
		if (!StringUtils.hasText(channelId)) {
			return;
		}

		channelSessionPool.computeIfAbsent(channelId, key -> new CopyOnWriteArraySet<>())
				.add(session);
	}

	/**
	 * WebSocket から受信したテキストメッセージを処理する。 クライアントから送信されたメッセージを解析し、保存とブロードキャストを行う。
	 *
	 * @param session WebSocket セッション
	 * @param textMessage 受信したテキストメッセージ
	 */
	@Override
	protected void handleTextMessage(@NonNull WebSocketSession session,
			@NonNull TextMessage textMessage) {
		try {
			MessageRequest request =
					OBJECT_MAPPER.readValue(textMessage.getPayload(), MessageRequest.class);

			handleFromMessage(request.getContent(), request.getChannelId());
			handleToMessage(request.getTo(), request.getContent(), request.getChannelId());

		} catch (IOException e) {
			logger.error("Failed to parse WebSocket message", e);
		} catch (Exception e) {
			logger.error("Error handling WebSocket message", e);
		}
	}

	/**
	 * WebSocket 接続が閉じられたときの処理。 該当チャネルのセッションプールからセッションを削除する。
	 *
	 * @param session 閉じられた WebSocket セッション
	 * @param status 閉鎖ステータス
	 * @throws Exception セッションクリーンアップに失敗した場合
	 */
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

	/**
	 * 画面表示用 DTO を JSON に変換して、同じチャネルの全セッションに送信する。
	 *
	 * @param messageResponse 送信対象のレスポンス DTO
	 * @throws JsonProcessingException JSON 変換に失敗した場合
	 * @throws IOException WebSocket 送信に失敗した場合
	 */
	public void sendMessage(MessageResponse messageResponse)
			throws JsonProcessingException, IOException {
		TextMessage sendTextMessage =
				new TextMessage(OBJECT_MAPPER.writeValueAsString(messageResponse));
		for (WebSocketSession webSocketSession : channelSessionPool
				.getOrDefault(messageResponse.getChannelId(), Collections.emptySet())) {
			webSocketSession.sendMessage(sendTextMessage);
		}
	}

	/**
	 * 送信元クライアントのメッセージを処理して、保存およびブロードキャストを行う。
	 *
	 * @param content メッセージ本文
	 * @param channelId チャネル ID
	 * @throws Exception 保存・送信処理に失敗した場合
	 */
	private void handleFromMessage(String content, String channelId) throws Exception {
		Message message = new Message();
		message.setContent(content);
		message.setChannelId(channelId);
		message.setCreatedAt(Instant.now().toString());
		message.setUserId(WS_USER_ID);

		saveMessage(message);
		sendMessage(MessageMapper.toMessageResponse(message));
	}

	/**
	 * 送信先フィールドが存在する場合に AI 応答を生成してブロードキャストする。
	 *
	 * @param to 宛先フィールド
	 * @param content 元メッセージ本文
	 * @param channelId チャネル ID
	 * @throws Exception AI 応答生成または送信処理に失敗した場合
	 */
	private void handleToMessage(String to, String content, String channelId) throws Exception {

		if (!StringUtils.hasText(to)) {
			return;
		}

		String response = chatClient.prompt().user(content).call().content();

		Message message = new Message();
		message.setContent(response);
		message.setChannelId(channelId);
		message.setCreatedAt(Instant.now().toString());
		message.setUserId(AI_USER_ID);

		saveMessage(message);
		sendMessage(MessageMapper.toMessageResponse(message));
	}

	/**
	 * WebSocket セッションからチャネル ID を抽出する。
	 *
	 * @param session WebSocket セッション
	 * @return クエリ文字列として指定されたチャネル ID、もしくは null
	 */
	private String getChannelId(@NonNull WebSocketSession session) {
		URI uri = session.getUri();
		return (uri == null) ? null : uri.getQuery();
	}

	/**
	 * メッセージを DynamoDB に保存する。
	 *
	 * @param message 保存対象のメッセージ
	 */
	private void saveMessage(Message message) {
		dynamoDbTemplate.save(message);
	}
}
