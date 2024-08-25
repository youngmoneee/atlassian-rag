package io.ymon.rag.config;

import io.ymon.rag.document.DocumentReader;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

@TestConfiguration
public class ETLConfig {

  @Bean
  DocumentReader atlassianDocumentReader() {
    return () -> Flux.just(
        Document.builder().withContent("atlassian").build()
    );
  }

  @Bean
  DocumentReader stackOverflowReader() {
    return () -> Flux.just(
        Document.builder().withContent("stackoverflow").build()
    );
  }

  @Bean
  @Primary
  DocumentReader entireDocumentReader(List<DocumentReader> documentReaders) {
    return () -> Flux.merge(documentReaders.stream().map(DocumentReader::get).toList());
  }
}
