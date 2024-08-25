package io.ymon.rag;

import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class VectorStoreTests {

  private final VectorStore vectorStore;

  @Autowired
  VectorStoreTests(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  @Test
  void initVectorStore() {
    assertThat(vectorStore).isNotNull();
  }

  @Test
  void queryTest() {

    List<Document> results = vectorStore.similaritySearch(
        SearchRequest.query("사용자의 질문에 대해 답변을 제공하는 서비스 데스크를 운영중입니다.\n"
                + "Task를 Jira로 관리하고 있는데, Task가 등록 되었을 경우 자사 챗봇을 통해 자동으로 응답하는 기능을 구현하려고 하고 있습니다.")
            .withTopK(3)
    );

    Logger.getLogger("Content").info(results.get(0).getContent());
  }
}
