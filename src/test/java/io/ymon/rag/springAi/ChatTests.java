package io.ymon.rag.springAi;

import io.ymon.rag.RagRetriever;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest
public class ChatTests {
  private final ChatClient client;
  private final RagRetriever retriever;

  @Autowired
  ChatTests(ChatClient client, RagRetriever retriever) {
    this.client = client;
    this.retriever = retriever;
  }

  @Test
  void injectTest() {
    assertThat(client).isNotNull();
    assertThat(retriever).isNotNull();
  }

  @Test
  void retrieveTest() {
    Flux<String> res = retriever.chat("1", "특정 조건을 통해 이슈를 자동으로 등록하고 싶습니다.");

    res.doOnNext(System.out::print).blockLast();
  }
}
