package io.ymon.rag.document;

import java.util.function.Function;
import reactor.core.publisher.Flux;

public interface Transformer<T, U> extends Function<Flux<T>, Flux<U>> {
  default Flux<U> transform(Flux<T> transform) { return apply(transform); }
}
