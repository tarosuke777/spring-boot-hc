package com.tarosuke777.hc.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tarosuke777.hc.dto.MessageRequest;
import com.tarosuke777.hc.dto.MessageResponse;
import com.tarosuke777.hc.handler.MessageHandler;
import com.tarosuke777.hc.service.MessageService;

@RestController
@CrossOrigin
public class MessageController {

	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
	private static final String DEFAULT_CHANNEL_ID = "1";
	private static final String WEBHOOK_USER_ID = "Jenkins-Bot";

	private final MessageHandler messageHandler;
	private final MessageService messageService;

	public MessageController(MessageHandler messageHandler, MessageService messageService) {
		this.messageHandler = messageHandler;
		this.messageService = messageService;
	}

	@GetMapping("/messages")
	public List<MessageResponse> index(
			@RequestParam(name = "channelId", defaultValue = DEFAULT_CHANNEL_ID) String channelId,
			@RequestParam(name = "toDateStr", required = false) String toDateStr) {

		return messageService.getMessagesByChannelId(channelId).stream()
				.map(messageService::toMessageResponse).collect(Collectors.toList());
	}

	@PostMapping("/messages/webhook")
	public void createFromWebhook(@RequestBody MessageRequest request) {
		try {
			MessageResponse response = messageService.createAndSaveMessage(request.getContent(),
					request.getChannelId(), WEBHOOK_USER_ID);
			messageHandler.sendMessage(response);
		} catch (Exception e) {
			logger.error("Failed to handle message from webhook", e);
		}
	}
}
