package com.tarosuke777.hc.controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
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

	@CrossOrigin
	@GetMapping("/messages")
	public List<Message> index(
			@RequestParam(name = "channelId", defaultValue = "1") String channelId,
			@RequestParam(name = "toDateStr", required = false) String toDateStr) {

		// channelIdでパーティションを特定します。
		QueryConditional keyEqual = QueryConditional.keyEqualTo(b -> b.partitionValue(channelId));

		// QueryEnhancedRequestで、ソート順序を逆にして、取得する最大件数を20に設定します。
		QueryEnhancedRequest tableQuery = QueryEnhancedRequest.builder().queryConditional(keyEqual)
				// ソートキーの降順（新しい順）で取得します。
				.scanIndexForward(false)
				// 取得する最大件数を20に設定します。
				.limit(20).build();

		PageIterable<Message> pages = dynamoDbTemplate.query(tableQuery, Message.class);
		Optional<Page<Message>> firstPage = pages.stream().findFirst();
		if (firstPage.isPresent()) {

			List<Message> messages = firstPage.get().items();
			Collections.reverse(messages);
			return messages;
		} else {
			return Collections.emptyList();
		}

	}

}
