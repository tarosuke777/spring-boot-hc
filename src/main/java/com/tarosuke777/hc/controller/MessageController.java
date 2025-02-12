package com.tarosuke777.hc.controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tarosuke777.hc.entity.Message;

import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@RestController
public class MessageController {

	@Autowired
	DynamoDbTemplate dynamoDbTemplate;

	@GetMapping("/messages")
	public List<Message> index(@RequestParam(name = "channelId", defaultValue = "1") String channelId) {

		QueryConditional keyEqual = QueryConditional.keyEqualTo(b -> b.partitionValue(channelId));
		QueryEnhancedRequest tableQuery = QueryEnhancedRequest.builder().queryConditional(keyEqual).build();
		PageIterable<Message> pages = dynamoDbTemplate.query(tableQuery, Message.class);
		// PageIterableが空の場合の対応を追加
		Optional<Page<Message>> firstPage = pages.stream().findFirst();
		if (firstPage.isPresent()) {
			return firstPage.get().items();
		} else {
			// 空の場合の処理 (例: 空のリストを返す、例外を投げる)
			return Collections.emptyList(); // 空のリストを返す例
			// throw new RuntimeException("No messages found for channelId: " + channelId);
			// // 例外を投げる例
		}

	}

}