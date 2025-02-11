package com.tarosuke777.hc.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

}
