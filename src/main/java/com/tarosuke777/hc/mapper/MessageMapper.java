package com.tarosuke777.hc.mapper;

import org.mapstruct.Mapper;
import com.tarosuke777.hc.dto.MessageResponse;
import com.tarosuke777.hc.entity.Message;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    MessageResponse toMessageResponse(Message message);
}
