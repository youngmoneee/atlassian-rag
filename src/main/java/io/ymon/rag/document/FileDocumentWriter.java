package io.ymon.rag.document;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.ymon.rag.DataType;
import io.ymon.rag.document.factory.QnA;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.DisposableBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class FileDocumentWriter implements DocumentWriter, DisposableBean {
  private final ObjectMapper om;
  private final PrintWriter writer;
  private final Consumer<QnA> qnaWriter;

  public FileDocumentWriter(PrintWriter writer) {
    this.om = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .disable(SerializationFeature.INDENT_OUTPUT);
    this.writer = writer;

    this.qnaWriter = qna -> {
      try {
        writer.println(om.writeValueAsString(qna));
      } catch (IOException e) {
        log.error(e.getMessage());
      }
    };
  }

  @Override
  public Flux<Document> apply(Flux<Document> documentFlux) {
    return documentFlux
        .flatMap(document -> Mono
        .just(document.getMetadata().get(DataType.QnA.getValue()))
        .cast(QnA.class)
        .doOnNext(qnaWriter)
        .doOnError(t -> log.info("Failed Write : {}", t.getMessage()))
        .onErrorComplete()
        .thenReturn(document));
  }

  @Override
  public void destroy() {
    writer.flush();
    writer.close();
    System.out.println("Resource Closed.");
  }
}
