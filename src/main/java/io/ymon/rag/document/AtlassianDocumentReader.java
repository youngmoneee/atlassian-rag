package io.ymon.rag.document;

import io.ymon.rag.DataType;
import io.ymon.rag.document.factory.QnA;
import io.ymon.rag.document.factory.AtlassianQnAFactory;
import io.ymon.rag.document.factory.QnAFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@Slf4j
public class AtlassianDocumentReader implements DocumentReader {
  private final Many<URI> emitter = Sinks.many().unicast().onBackpressureBuffer();
  private final Flux<Document> documentFlux;

  private static final int MAX_CONCURRENCY = 5;

  @Override
  public Flux<Document> get() {
    return documentFlux;
  }

  public AtlassianDocumentReader(List<URI> urlList, Transformer<URI, Element> toElementFlux) {
    this(Flux.fromIterable(urlList), toElementFlux);
  }

  public AtlassianDocumentReader(Flux<URI> urlFlux, Transformer<URI, Element> toElementFlux) {
    this.documentFlux = emitter.asFlux()
        .mergeWith(urlFlux) //  merge Entry Point URLs
        .transform(toElementFlux)
        .flatMap(documentation);  //  convert to spring.ai.Document
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

  private final QnAFactory convertToQnA = new AtlassianQnAFactory();

  private Function<Element, Mono<Document>> documentation = document -> {
    if (Objects.nonNull(document.selectFirst("meta[property=og:type][content=article]")))
      return Mono.just(document).mapNotNull(convertToQnA).map(convertToDocument)
          .onErrorResume(t -> Mono.empty());

    return Flux.fromIterable(
            document.select("a[rel=next], h3.atl-post-list__tile__title > a"))
        .mapNotNull(convertToUri).doOnNext(emitter::tryEmitNext)
        .onErrorResume(t -> Mono.empty())
        .then(Mono.empty());
  };
}
