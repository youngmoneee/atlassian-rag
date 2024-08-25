package io.ymon.rag.document;

import java.util.function.Supplier;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

public interface DocumentReader extends Supplier<Flux<Document>> {
  default Flux<Document> read() {
    return get();
  }
}
