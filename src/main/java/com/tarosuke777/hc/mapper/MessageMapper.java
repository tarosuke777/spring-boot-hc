package com.tarosuke777.hc.mapper;

import com.tarosuke777.hc.dto.MessageResponse;
import com.tarosuke777.hc.entity.Message;

public final class MessageMapper {

    private MessageMapper() {
        // Utility class
    }

    public static MessageResponse toMessageResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setChannelId(message.getChannelId());
        response.setCreatedAt(message.getCreatedAt());
        response.setContent(message.getContent());
        response.setUserId(message.getUserId());
        return response;
    }
}
