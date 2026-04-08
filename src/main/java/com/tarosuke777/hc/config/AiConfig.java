package com.tarosuke777.hc.config;

import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {

    @Autowired
    ChatMemory chatMemory;

    @Autowired
    private RestClient restClient;

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        try {
            // HMSのAPIからリストを取得
            List<BookDto> hmsBooks = restClient.get().uri("/api/books").retrieve()
                    .body(new ParameterizedTypeReference<List<BookDto>>() {});

            // HMSのデータをDocumentに変換
            if (hmsBooks != null) {
                hmsBooks.forEach(book -> {
                    // 文章の組み立てにリンクを含める
                    StringBuilder content = new StringBuilder();
                    content.append(String.format("蔵書情報: タイトルは「%s」、ジャンルは「%s」です。", book.name(),
                            book.genre()));

                    if (book.link() != null && !book.link().isBlank()) {
                        content.append(String.format(" 詳細リンクはこちら: %s", book.link()));
                    }

                    Document doc = new Document(content.toString(), Map.of("bookId", book.id()));

                    try {
                        store.add(List.of(doc));
                    } catch (Exception e) {
                        System.err.println("この本の登録に失敗しました: " + book.name());
                        // 特定の本が長すぎる場合はスキップして次へ
                    }

                });
            }

        } catch (Exception e) {
            // HMSが落ちていてもHCが起動するようにエラーハンドリング
            System.err.println("HMSからのデータ取得に失敗しました: " + e.getMessage());
        }

        return store;

        // List<Document> documents = List.of(new Document("tarosuke777の趣味は、開発です。"),
        // new Document("Home Chatプロジェクトは、JavaとSpring AIで構築されています。"));
        // store.add(documents);
        // return store;
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, VectorStore vectorStore) {
        return builder.defaultSystem(
                "あなたは関西出身のフレンドリーなアシスタントです。回答はすべて自然な関西弁で行ってください。蔵書について聞かれた際、URLの情報があれば必ず含めて回答してください。挨拶は抜きで、簡潔に頼みます。")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
    }

    // HMSのBookFormに合わせた受信用DTO
    public record BookDto(Integer id, String name, // 本のタイトル
            String genre, // EnumはStringで受け取れます
            String note, // メモ
            String link // 関連リンク（例：AmazonのURLなど）
    ) {
    }

}
