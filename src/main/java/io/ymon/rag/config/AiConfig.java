package io.ymon.rag.config;

import io.ymon.rag.QnaAdvisor;
import io.ymon.rag.RagRetriever;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class AiConfig {
  private final static String SYSTEM_MESSAGE = """
      당신은 Atlassian 제품의 사용자가 겪는 문제를 해결해야합니다.
      답변은 마크다운 포맷의 한국어로 생성되어야하며, 해결 방안을 단계로 나누고 필요하다면 문제 해결을 위한 예시나 스크립트를 제공할 수 있습니다.
      만약 3rd-party를 통해 해결해야한다면, 가능한 여려 대안들과 각각의 장단점 및 사용 예시에 대해 설명해주세요
      """;

  @Bean
  ChatMemory chatMemory() {
    return new InMemoryChatMemory();
  }

  @Bean
  QnaAdvisor qnaAdvisor() {
    return new QnaAdvisor(SearchRequest.defaults().withTopK(4));
  }

  @Bean
  MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory memory) {
    return new MessageChatMemoryAdvisor(memory, "default", 5);
  }

  @Bean
  @Order(Integer.MAX_VALUE)
  SimpleLoggerAdvisor loggerAdvisor() {
    return new SimpleLoggerAdvisor(
        req -> "User's Text : " + req.userText() + "\n\n"
            + "User's Parameters : " + req.userParams() + "\n\n",
        res -> ""
    );
  }

  @Bean
  ChatClient chatClient(ChatModel model, List<RequestResponseAdvisor> advisors) {
    return ChatClient.builder(model)
        .defaultSystem(SYSTEM_MESSAGE)
        .defaultAdvisors(advisors)
        .build();
  }

  @Bean
  RagRetriever retriever(ChatClient client, VectorStore vectorStore) {
    return new RagRetriever(client, vectorStore);
  }
}
