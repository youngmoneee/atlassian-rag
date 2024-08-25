package io.ymon.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import reactor.core.publisher.Flux;

public class QnaAdvisor implements RequestResponseAdvisor {
  private final static String CONTEXT = "question_answer_context";
  private final static String RETRIEVED_DOCUMENTS = QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS;
  private final static String REFERENCED_URLS = "referenced_urls";
  private final static String DEFAULT_USER_TEXT_ADVISE =
      String.format(
          """
          
          아래 Markdown 문서들은 사용자가 겪는 문제와 유사한 Context입니다.
          ---------------------
          {%s}
          ---------------------
          사용자의 질문과 함께 주어진 Context는 다른 사용자가 겪은 유사한 문제와 이에 대한 답변이며, 사전 지식보다 우선적으로 참고되어야 합니다.
          
          """, CONTEXT);

  private final SearchRequest DEFAULT_SEARCH_REQUEST;
  private final String ADVISE_TEXT;

  public QnaAdvisor() {
    this(SearchRequest.defaults(), DEFAULT_USER_TEXT_ADVISE);
  }

  public QnaAdvisor(SearchRequest searchRequest) {
    this(searchRequest, DEFAULT_USER_TEXT_ADVISE);
  }

  public QnaAdvisor(SearchRequest searchRequest, String userTextAdvise) {
    this.DEFAULT_SEARCH_REQUEST = searchRequest;
    this.ADVISE_TEXT = userTextAdvise;
  }

  @Override
  public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
    AdvisedRequest.Builder result = AdvisedRequest.from(request);

    try {
      if (request.advisorParams().get("documents") instanceof Callable<?> callable) {
        if (callable.call() instanceof List<?> documents) {

          @SuppressWarnings("unchecked")
          List<MetaData> metaDataList =
              ((List<Document>)documents).stream()
              .map(Document::getMetadata)
              .map(MetaData::of).toList();

          String advisedText = request.userText() + System.lineSeparator() + ADVISE_TEXT;

          Map<String, Object> userParams = new HashMap<>(request.userParams());
          userParams.put(CONTEXT, metaDataList.stream().map(MetaData::document)
              .collect(Collectors.joining(System.lineSeparator())));
          context.put(REFERENCED_URLS, metaDataList.stream().map(MetaData::url).toList());

          result
              .withUserText(advisedText)
              .withUserParams(userParams);
        }
      }
    } catch (Exception e){
      return request;
    }

    return result.build();
  }

  @Override
  public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
    return ChatResponse.builder().from(response)
        .withMetadata(REFERENCED_URLS, context.get(REFERENCED_URLS))
        .build();
  }

  @Override
  public Flux<ChatResponse> adviseResponse(Flux<ChatResponse> fluxResponse, Map<String, Object> context) {
    return fluxResponse.map(cr -> ChatResponse.builder().from(cr)
        .withMetadata(RETRIEVED_DOCUMENTS, context.get(RETRIEVED_DOCUMENTS))
        .build()
    );
  }

  @SuppressWarnings("unchecked")
  private record MetaData(String document, String url, List<String> tags) {
    static MetaData of(Map<String, Object> map) {
      String document = (String) map.get(DataType.DOCUMENT.getValue());
      String url = (String) map.get(DataType.URL.getValue());
      List<String> tags = (List<String>) map.get(DataType.TAGS.getValue());
      return new MetaData(document, url, tags);
    }
  }
}
