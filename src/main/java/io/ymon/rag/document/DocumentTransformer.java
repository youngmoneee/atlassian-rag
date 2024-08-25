package io.ymon.rag.document;

import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

public interface DocumentTransformer extends Transformer<Document, Document> {
  default Flux<Document> transform(Flux<Document> transform) { return apply(transform); }
}