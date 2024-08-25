package io.ymon.rag.document;

import io.ymon.rag.DataType;
import io.ymon.rag.document.factory.QnA;
import io.ymon.rag.document.factory.QnAFactory;
import io.ymon.rag.document.factory.StackOverflowQnAFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.function.Function;
import org.jsoup.nodes.Element;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

public class StackOverflowDocumentReader implements DocumentReader {
  private final Many<URI> emitter = Sinks.many().unicast().onBackpressureBuffer();
  private final Flux<Document> documentFlux;

  public StackOverflowDocumentReader(Flux<URI> urlFlux, Transformer<URI, Element> toElementFlux) {
    this.documentFlux = emitter.asFlux()
        .mergeWith(urlFlux) //  merge Entry Point URLs
        .transform(toElementFlux)
        .flatMap(documentation);
  }

  @Override
  public Flux<Document> get() {
    return this.documentFlux;
  }

  private final Function<Element, URI> convertToUri = elem -> {
    try {
      return new URI(elem.absUrl("href"));
    } catch (URISyntaxException ignored) {
      return null;
    }
  };

  private final Function<QnA, Document> convertToDocument = qna ->
      Document.builder()
          .withContent(qna.question().toString())
          .withMetadata(DataType.QnA.getValue(), qna)
          //.withMetadata(DataType.DOCUMENT.getValue(), qna.toString())
          //.withMetadata(DataType.URL.getValue(), qna.url())
          //.withMetadata(DataType.TAGS.getValue(), qna.tags())
          .build();

  private final QnAFactory convertToQnA = new StackOverflowQnAFactory();

  private Function<Element, Mono<Document>> documentation = document -> {
    if (Objects.isNull(document.getElementById("questions")))
      return Mono.just(document).mapNotNull(convertToQnA).map(convertToDocument).onErrorResume(t -> Mono.empty());

    return Flux.fromIterable(
            document.select("a[rel=next], h3.s-post-summary--content-title > a"))
        .mapNotNull(convertToUri).doOnNext(emitter::tryEmitNext)
        .onErrorResume(t -> Mono.empty())
        .then(Mono.empty());
  };
}
