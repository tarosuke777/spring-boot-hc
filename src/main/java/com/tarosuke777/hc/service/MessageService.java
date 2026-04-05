package com.tarosuke777.hc.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tarosuke777.hc.dto.MessageResponse;
import com.tarosuke777.hc.entity.Message;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

/**
 * チャットメッセージの取得と永続化を処理するサービスクラスです。
 *
 * <p>
 * このサービスは、指定されたチャネルのメッセージ履歴を取得し、 永続化された {@link Message} エンティティを {@link MessageResponse} に変換します。
 * </p>
 */
@Service
public class MessageService {

    private static final int MAX_MESSAGE_LIMIT = 20;
    private final DynamoDbTemplate dynamoDbTemplate;

    public MessageService(DynamoDbTemplate dynamoDbTemplate) {
        this.dynamoDbTemplate = dynamoDbTemplate;
    }

    /**
     * 指定したチャネルの最新メッセージを取得します。
     *
     * @param channelId 検索対象のチャネルID
     * @return 古い順にソートされたメッセージリスト（最大 {@code MAX_MESSAGE_LIMIT} 件）
     */
    public List<Message> getMessagesByChannelId(String channelId) {

        QueryConditional keyEqual = QueryConditional.keyEqualTo(b -> b.partitionValue(channelId));

        QueryEnhancedRequest tableQuery = QueryEnhancedRequest.builder().queryConditional(keyEqual)
                .scanIndexForward(false).limit(MAX_MESSAGE_LIMIT).build();

        List<Message> messages = dynamoDbTemplate.query(tableQuery, Message.class).stream()
                .findFirst().map(Page::items).orElse(Collections.emptyList());

        Collections.reverse(messages);
        return messages;
    }

    /**
     * 新しいメッセージを作成して保存し、保存後のレスポンスDTOを返します。
     *
     * @param content メッセージ本文
     * @param channelId メッセージの所属するチャネルID
     * @param userId 送信者のユーザーID
     * @return 保存されたメッセージを変換した {@link MessageResponse}
     */
    public MessageResponse createAndSaveMessage(String content, String channelId, String userId) {
        Message message = new Message();
        message.setContent(content);
        message.setChannelId(channelId);
        message.setCreatedAt(Instant.now().toString());
        message.setUserId(userId);
        dynamoDbTemplate.save(message);
        return toMessageResponse(message);
    }

    /**
     * 永続化済みのメッセージエンティティをレスポンスDTOに変換します。
     *
     * @param message 永続化されたメッセージエンティティ
     * @return API や WebSocket 送信で使用可能なメッセージレスポンス
     */
    public MessageResponse toMessageResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setChannelId(message.getChannelId());
        response.setCreatedAt(message.getCreatedAt());
        response.setContent(message.getContent());
        response.setUserId(message.getUserId());
        return response;
    }
}
