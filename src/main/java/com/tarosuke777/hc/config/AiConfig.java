package com.tarosuke777.hc.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class AiConfig {

        private final ChatMemory chatMemory;

        private final VectorStore vectorStore;

        @Bean
        public ChatClient chatClient(ChatClient.Builder builder) {

                var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
                var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(6).build()).build();

                return builder.defaultSystem(
                                "あなたは関西出身のフレンドリーなアシスタントです。回答はすべて自然な関西弁で行ってください。蔵書について聞かれた際、URLの情報があれば必ず含めて回答してください。挨拶は抜きで、簡潔に頼みます。")
                                .defaultAdvisors(memoryAdvisor, qaAdvisor).build();
        }
}
