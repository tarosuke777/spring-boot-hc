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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
public class MessageController {

	private final MessageHandler messageHandler;
	private final MessageService messageService;

	/**
	 * 指定されたチャネル ID に関連するメッセージのリストを取得します。
	 *
	 * @param channelId メッセージを取得するチャネルの ID
	 * @return 指定されたチャネルに関連するメッセージのリスト
	 */
	@GetMapping("/messages")
	public List<MessageResponse> index(
			@RequestParam(name = "channelId", defaultValue = "1") String channelId) {

		return messageService.getMessagesByChannelId(channelId).stream()
				.map(messageService::toMessageResponse).collect(Collectors.toList());
	}

	/**
	 * Webhook からのメッセージを処理し、保存してブロードキャストします。
	 *
	 * @param request Webhook から送信されたメッセージリクエスト
	 */
	@PostMapping("/messages/webhook")
	public void createFromWebhook(@RequestBody MessageRequest request) {

		try {
			MessageResponse response = messageService.createAndSaveMessage(request.getContent(),
					request.getChannelId(), "Jenkins-Bot");
			messageHandler.sendMessage(response);
		} catch (Exception e) {
			log.error("Failed to handle message from webhook", e);
		}
	}
}
