package com.tarosuke777.hc.controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tarosuke777.hc.entity.Channel;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

@RestController
public class ChannelController {

	@Autowired
	DynamoDbTemplate dynamoDbTemplate;

	@CrossOrigin
	@GetMapping("/channels")
	public List<Channel> index() {
		PageIterable<Channel> pages = dynamoDbTemplate.scanAll(Channel.class);
		// PageIterableが空の場合の対応を追加
		Optional<Page<Channel>> firstPage = pages.stream().findFirst();
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
