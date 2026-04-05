package com.tarosuke777.hc.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tarosuke777.hc.entity.Message;
import com.tarosuke777.hc.handler.MessageHandler;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@RestController
@CrossOrigin
public class MessageController {

	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
	private static final String DEFAULT_CHANNEL_ID = "1";
	private static final String DEFAULT_MESSAGE_CONTENT = "No message content";
	private static final String WEBHOOK_USER_ID = "Jenkins-Bot";
	private static final int MAX_MESSAGE_LIMIT = 20;

	private final MessageHandler messageHandler;
	private final DynamoDbTemplate dynamoDbTemplate;

	public MessageController(MessageHandler messageHandler, DynamoDbTemplate dynamoDbTemplate) {
		this.messageHandler = messageHandler;
		this.dynamoDbTemplate = dynamoDbTemplate;
	}

	@GetMapping("/messages")
	public List<Message> index(
			@RequestParam(name = "channelId", defaultValue = DEFAULT_CHANNEL_ID) String channelId,
			@RequestParam(name = "toDateStr", required = false) String toDateStr) {

		QueryConditional keyEqual = QueryConditional.keyEqualTo(b -> b.partitionValue(channelId));

		QueryEnhancedRequest tableQuery = QueryEnhancedRequest.builder().queryConditional(keyEqual)
				.scanIndexForward(false).limit(MAX_MESSAGE_LIMIT).build();

		List<Message> messages = dynamoDbTemplate.query(tableQuery, Message.class).stream()
				.findFirst().map(Page::items).orElse(Collections.emptyList());

		Collections.reverse(messages);
		return messages;
	}

	@PostMapping("/messages/webhook")
	public Message createFromWebhook(@RequestBody Map<String, String> payload) {

		Message message = buildMessageFromWebhookPayload(payload);
		saveAndBroadcastMessage(message);
		return message;
	}

	private Message buildMessageFromWebhookPayload(Map<String, String> payload) {
		Message message = new Message();
		message.setContent(payload.getOrDefault("content", DEFAULT_MESSAGE_CONTENT));
		message.setChannelId(payload.getOrDefault("channelId", DEFAULT_CHANNEL_ID));
		message.setCreatedAt(Instant.now().toString());
		message.setUserId(WEBHOOK_USER_ID);
		return message;
	}

	private void saveAndBroadcastMessage(Message message) {
		dynamoDbTemplate.save(message);
		try {
			messageHandler.sendMessage(message);
		} catch (Exception e) {
			logger.warn("Failed to broadcast message to WebSocket clients", e);
		}
	}
}
