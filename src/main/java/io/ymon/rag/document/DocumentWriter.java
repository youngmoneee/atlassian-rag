package io.ymon.rag.document;

import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

public interface DocumentWriter extends Transformer<Document, Document> {
  default void write(Flux<Document> documents) { apply(documents); }
}
