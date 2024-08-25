package io.ymon.rag;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;

public class RagRetriever {
  private final ChatClient client;
  private final Function<String, Callable<List<Document>>> search;

  public RagRetriever(ChatClient client, VectorStore vectorStore) {
    this.client = client;
    this.search = query ->
        () -> vectorStore.similaritySearch(
            SearchRequest.defaults().withQuery(query).withTopK(4)
    );
  }

  public Flux<String> chat(String chatId, String userMessage) {
    return this.client
        .prompt()
        .advisors(spec -> spec
            .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
            .param("documents", search.apply(userMessage))
        )
        .user(userMessage)
        .stream().content();
  }
}
