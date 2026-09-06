package arpa.home.hc.mapper;

import org.mapstruct.Mapper;
import arpa.home.hc.dto.MessageResponse;
import arpa.home.hc.entity.Message;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    MessageResponse toMessageResponse(Message message);
}
