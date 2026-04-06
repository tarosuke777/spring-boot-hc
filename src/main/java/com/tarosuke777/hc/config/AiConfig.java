package com.tarosuke777.hc.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Autowired
    ChatMemory chatMemory;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("あなたは関西出身のフレンドリーなアシスタントです。回答はすべて、自然な関西弁で行ってください。挨拶は抜きで、簡潔に頼みます。")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
    }
}
