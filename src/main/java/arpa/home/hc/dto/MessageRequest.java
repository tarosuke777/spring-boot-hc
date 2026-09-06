package arpa.home.hc.dto;

import lombok.Data;

@Data
public class MessageRequest {
    private String channelId;
    private String content;
    private String to;
}
