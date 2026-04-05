package com.tarosuke777.hc.controller;

import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tarosuke777.hc.entity.Channel;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

@RestController
@CrossOrigin
public class ChannelController {

	private final DynamoDbTemplate dynamoDbTemplate;

	public ChannelController(DynamoDbTemplate dynamoDbTemplate) {
		this.dynamoDbTemplate = dynamoDbTemplate;
	}

	@GetMapping("/channels")
	public List<Channel> index() {
		return dynamoDbTemplate.scanAll(Channel.class).stream().findFirst().map(Page::items)
				.orElse(Collections.emptyList());
	}
}
