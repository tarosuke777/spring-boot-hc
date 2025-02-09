package com.tarosuke777.hc.entity;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@DynamoDbBean
public class Message {
	private String channelId;
	private String createdAt;
	private String content;
	private String userId;
	
	@DynamoDbPartitionKey
	public String getChannelId() {
		return channelId;
	}

	@DynamoDbSortKey
	public String getCreatedAt() {
		return createdAt;
	}

}
