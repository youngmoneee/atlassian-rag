package io.ymon.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class SimpleController {

  private final RagRetriever retriever;

  @Value("classpath:/static/test.html")
  private Resource index;

  SimpleController(RagRetriever retriever) { this.retriever = retriever; }

  @GetMapping
  ResponseEntity<Resource> testIndex() {
    return ResponseEntity.ok(index);
  }

  @PostMapping("/question")
  Flux<String> test(@RequestBody String userText) {
    return retriever.chat("test", userText);
  }
}