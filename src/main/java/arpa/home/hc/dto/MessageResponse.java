package arpa.home.hc.dto;

import lombok.Data;

@Data
public class MessageResponse {
    private String channelId;
    private String createdAt;
    private String content;
    private String userId;
}
