package com.tarosuke777.hc.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import com.tarosuke777.hc.entity.Channel;
import com.tarosuke777.hc.entity.Greeting;
import com.tarosuke777.hc.entity.Message;

import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Controller
public class IndexController {

	@Autowired
	DynamoDbTemplate dynamoDbTemplate;

	@GetMapping("/")
	public String index(@RequestParam(name = "channelId", defaultValue = "1") String channelId, Model model) {

		QueryConditional keyEqual = QueryConditional.keyEqualTo(b -> b.partitionValue(channelId));
		QueryEnhancedRequest tableQuery = QueryEnhancedRequest.builder().queryConditional(keyEqual).build();
		PageIterable<Message> pages = dynamoDbTemplate.query(tableQuery, Message.class);
		List<Message> messages = pages.stream().findFirst().get().items();

		model.addAttribute("channelId", channelId);
		model.addAttribute("messages", messages);
		return "index";
	}

	@MessageMapping("/message")
	@SendTo("/topic/message")
	public Message message(Message message) throws Exception {

		message.setChannelId(message.getChannelId());
		message.setCreatedAt(LocalDateTime.now().toString());
		message.setUserId("1");

		dynamoDbTemplate.save(message);

		return message;
	}
}
