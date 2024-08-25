package io.ymon.rag.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.WeaviateVectorStore;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class WeaviateDocumentWriter implements DocumentWriter { //  Wrapper

  private final VectorStore vectorStore;

  public WeaviateDocumentWriter(WeaviateVectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  @Override
  public Flux<Document> apply(Flux<Document> documentFlux) {
    return documentFlux
        .window(50) //  Batch Size
        .flatMap(Flux::collectList)
        .flatMap(list -> Mono
            .fromRunnable(() -> vectorStore.add(list))
            .retry(2)
            .doOnError(t -> log.error("Insert Error : {}", t.getMessage()))
            .onErrorReturn(list)
            .thenReturn(list)
            .subscribeOn(Schedulers.boundedElastic()), 50, 10
        )
        .flatMap(Flux::fromIterable);
  }
}
