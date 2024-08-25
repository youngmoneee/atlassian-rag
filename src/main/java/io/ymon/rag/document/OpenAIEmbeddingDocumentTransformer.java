package io.ymon.rag.document;

import io.ymon.rag.DataType;
import java.time.Duration;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class OpenAIEmbeddingDocumentTransformer implements DocumentTransformer {

  private final Function<Document, Mono<Document>> embedding;

  public OpenAIEmbeddingDocumentTransformer(OpenAiEmbeddingModel model) {
    this.embedding = document -> Mono.zip(
        Mono.just(document),
        Mono.fromCallable(() -> model.embed(document)),
        (doc, embed) -> {
          doc.setEmbedding(embed);
          return doc;
        }
    )
    .onErrorResume(t -> {
      log.warn("Embedding Error - URL is {}", document.getMetadata().getOrDefault(DataType.URL.getValue(), ""));
      return Mono.empty();
    }).subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Flux<Document> apply(Flux<Document> documentFlux) {
    return documentFlux
        .delayElements(Duration.ofMillis(60000 / 1500)) //  1분에 요청 1500개
        .flatMap(embedding, 1000, 100);
  }

}