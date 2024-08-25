package io.ymon.rag;

import io.ymon.rag.config.ETLConfig;
import io.ymon.rag.document.DocumentReader;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(ETLConfig.class)
public class DocumentReaderTests {
  private final DocumentReader documentReader;

  @Autowired
  DocumentReaderTests(DocumentReader documentReader) {
    this.documentReader = documentReader;
  }

  @Test
  void injectionTest() {
    assertThat(documentReader).isNotNull();
    assertThat(documentReader.read()).isInstanceOf(Flux.class);
  }

  @Test
  void readTest() {
    StepVerifier.create(documentReader.read().collectList())
        .assertNext(documents -> {
          assertThat(documents).isNotNull();
          assertThat(documents).hasSize(2); //  merge
          assertThat(documents).extracting(Document::getContent)
              .contains("atlassian", "stackoverflow");  //  value
        })
        .verifyComplete();
  }
}
