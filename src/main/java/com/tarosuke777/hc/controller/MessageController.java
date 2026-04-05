package com.tarosuke777.hc.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tarosuke777.hc.dto.MessageRequest;
import com.tarosuke777.hc.dto.MessageResponse;
import com.tarosuke777.hc.entity.Message;
import com.tarosuke777.hc.handler.MessageHandler;
import com.tarosuke777.hc.mapper.MessageMapper;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@RestController
@CrossOrigin
public class MessageController {

	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
	private static final String DEFAULT_CHANNEL_ID = "1";
	private static final String WEBHOOK_USER_ID = "Jenkins-Bot";
	private static final int MAX_MESSAGE_LIMIT = 20;

	private final MessageHandler messageHandler;
	private final DynamoDbTemplate dynamoDbTemplate;

	public MessageController(MessageHandler messageHandler, DynamoDbTemplate dynamoDbTemplate) {
		this.messageHandler = messageHandler;
		this.dynamoDbTemplate = dynamoDbTemplate;
	}

	@GetMapping("/messages")
	public List<MessageResponse> index(
			@RequestParam(name = "channelId", defaultValue = DEFAULT_CHANNEL_ID) String channelId,
			@RequestParam(name = "toDateStr", required = false) String toDateStr) {

		QueryConditional keyEqual = QueryConditional.keyEqualTo(b -> b.partitionValue(channelId));

		QueryEnhancedRequest tableQuery = QueryEnhancedRequest.builder().queryConditional(keyEqual)
				.scanIndexForward(false).limit(MAX_MESSAGE_LIMIT).build();

		List<Message> messages = dynamoDbTemplate.query(tableQuery, Message.class).stream()
				.findFirst().map(Page::items).orElse(Collections.emptyList());

		Collections.reverse(messages);
		return messages.stream().map(MessageMapper::toMessageResponse).collect(Collectors.toList());
	}

	@PostMapping("/messages/webhook")
	public MessageResponse createFromWebhook(@RequestBody MessageRequest request) {
		Message message = buildMessageFromRequest(request, WEBHOOK_USER_ID);
		MessageResponse response = MessageMapper.toMessageResponse(message);
		saveAndBroadcastMessage(response);
		return response;
	}

	private Message buildMessageFromRequest(MessageRequest request, String userId) {
		Message message = new Message();
		message.setContent(StringUtils.hasText(request.getContent()) ? request.getContent() : "");
		message.setChannelId(StringUtils.hasText(request.getChannelId()) ? request.getChannelId()
				: DEFAULT_CHANNEL_ID);
		message.setCreatedAt(Instant.now().toString());
		message.setUserId(userId);
		return message;
	}

	private void saveAndBroadcastMessage(MessageResponse response) {
		Message entity = buildMessageFromResponse(response);
		dynamoDbTemplate.save(entity);
		try {
			messageHandler.sendMessage(response);
		} catch (Exception e) {
			logger.warn("Failed to broadcast message to WebSocket clients", e);
		}
	}

	private Message buildMessageFromResponse(MessageResponse response) {
		Message message = new Message();
		message.setChannelId(response.getChannelId());
		message.setContent(response.getContent());
		message.setCreatedAt(response.getCreatedAt());
		message.setUserId(response.getUserId());
		return message;
	}
}
