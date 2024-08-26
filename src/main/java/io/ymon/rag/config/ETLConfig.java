package io.ymon.rag.config;

import io.ymon.rag.DataType;
import io.ymon.rag.document.AtlassianDocumentReader;
import io.ymon.rag.document.DocumentReader;
import io.ymon.rag.document.DocumentTransformer;
import io.ymon.rag.document.DocumentWriter;
import io.ymon.rag.document.EmbeddingDocumentTransformer;
import io.ymon.rag.document.FileDocumentWriter;
import io.ymon.rag.document.StackOverflowDocumentReader;
import io.ymon.rag.document.Transformer;
import io.ymon.rag.document.WeaviateDocumentWriter;
import io.ymon.rag.annotation.EntryPoint;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.WeaviateVectorStore;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Slf4j
@Configuration
@Conditional(ScrapeCondition.class)
public class ETLConfig implements DisposableBean {

  private final long start = System.currentTimeMillis();
  private Disposable disposable = null;

  @Override
  public void destroy() {
    if (disposable != null && !disposable.isDisposed())
      disposable.dispose();
    log.info("Duration : {} sec", (double)(System.currentTimeMillis() - start) / 1000);
  }

  @Bean
  ApplicationRunner scrape(ApplicationContext context, Flux<Document> flux) {
    return args -> {
      this.disposable = flux
          .onErrorResume(t -> {
            log.error(t.getMessage());
            return Mono.empty();
          })
          .doFinally(signal -> {
            SpringApplication.exit(context, () -> signal.equals(SignalType.ON_COMPLETE) ? 0 : 1);
          })
          .subscribe();
    };
  }

  @Bean
  AtlassianDocumentReader atlassianDocumentReader(
      @EntryPoint.Atlassian List<Flux<URI>> fluxes,
      Transformer<URI, Element> transformer) {
    return new AtlassianDocumentReader(Flux.merge(fluxes), transformer);
  }

  @Bean
  StackOverflowDocumentReader stackOverflowDocumentReader(
      @EntryPoint.Stackoverflow List<Flux<URI>> fluxes,
      Transformer<URI, Element> transformer) {
    return new StackOverflowDocumentReader(Flux.merge(fluxes), transformer);
  }

  @Bean
  @Primary
  DocumentReader entireDocumentReader(List<DocumentReader> documentReaders) {
    return () -> Flux.merge(documentReaders.stream().map(DocumentReader::get).toList());
  }

  //@Bean
  @Order()
  @ConditionalOnBean(EmbeddingModel.class)
  DocumentTransformer transformer(EmbeddingModel model) {
    return new EmbeddingDocumentTransformer(model);
  }

  @Bean
  @Primary
  DocumentTransformer transformation(List<DocumentTransformer> transformers) {
    return documentFlux -> transformers.stream()
        .reduce(
            documentFlux,
            Flux::transform,
            (before, after) -> after
        );
  }

  //@Bean
  //@Primary
  DocumentWriter weaviateDocumentWriter(WeaviateVectorStore vectorStore) {
    return new WeaviateDocumentWriter(vectorStore);
  }

  //@Bean
  DocumentWriter fileDocumentWriter() throws IOException {
    return new FileDocumentWriter(new PrintWriter(new BufferedWriter(new FileWriter("dataset.jsonl"))));
  }

  @ConditionalOnMissingBean
  @Bean
  DocumentWriter logDocumentWriter() {
    Logger log = LoggerFactory.getLogger(DocumentWriter.class);
    return documentFlux -> documentFlux.doOnNext(doc -> log.info("{}", doc.getMetadata().get(
        DataType.QnA.getValue())));
  }

  @Bean
  Flux<Document> stream(DocumentReader reader, DocumentTransformer transformer, DocumentWriter write) {
    return reader
        .read()
        .transform(transformer)
        .transform(write);
  }

  @Bean
  @EntryPoint.Atlassian
  Flux<URI> atlassianEntryPoint() throws URISyntaxException {
    return Flux.just(
        new URI("https://community.atlassian.com/t5/Confluence-questions/qa-p/confluence-questions"), //  conf
        new URI("https://community.atlassian.com/t5/Questions-for-Confluence/qa-p/questions-for-confluence-questions"), //  Question for Confluence
        new URI("https://community.atlassian.com/t5/Team-Calendars-for-Confluence/qa-p/team-calendars-confluence-questions"), //  Team calendars for Confluence
        new URI("https://community.atlassian.com/t5/Atlassian-Account-questions/qa-p/account-identity-questions"),  //  Atlassian Accounts
        new URI("https://community.atlassian.com/t5/Jira-questions/qa-p/jira-questions"),  //  jira
        new URI("https://community.atlassian.com/t5/Jira-Align-questions/qa-p/jira-align-questions"), //  Jira Align
        new URI("https://community.atlassian.com/t5/Jira-Work-Management-Questions/qa-p/jira-work-management-questions"), //  Jira Work Management
        new URI("https://community.atlassian.com/t5/Advanced-planning-questions/qa-p/portfolio-for-jira-questions"),  //  Jira Advanced Planning
        new URI("https://community.atlassian.com/t5/Jira-Mobile-Apps-questions/qa-p/jira-mobile-apps-questions"), //  Jira Mobile Apps
        new URI("https://community.atlassian.com/t5/Jira-Product-Discovery-questions/qa-p/jpd-questions"), // Jira Product Discovery
        new URI("https://community.atlassian.com/t5/Assist-questions/qa-p/halp-questions"), //  Jira Assist
        new URI("https://community.atlassian.com/t5/Compass-questions/qa-p/compass-questions"), //  Compass
        new URI("https://community.atlassian.com/t5/Trello-questions/qa-p/trello-questions"), //  Trello
        new URI("https://community.atlassian.com/t5/Atlas-questions/qa-p/atlas-questions"), //  Atlas
        new URI("https://community.atlassian.com/t5/Pipelines-questions/qa-p/pipelines_questions"), //  Pipelines
        new URI("https://community.atlassian.com/t5/Opsgenie-questions/qa-p/opsgenie-questions"), //  Ops-genie
        new URI("https://community.atlassian.com/t5/Bamboo-questions/qa-p/bamboo-questions"), //  Bamboo
        new URI("https://community.atlassian.com/t5/Crowd-questions/qa-p/crowd-questions"), //  Crowd
        new URI("https://community.atlassian.com/t5/Statuspage-questions/qa-p/statuspage-questions"), //  Statuspage
        new URI("https://community.atlassian.com/t5/Fisheye-Crucible-questions/qa-p/fisheye-crucible-questions"), //  Fisheye
        new URI("https://community.atlassian.com/t5/Automation-questions/qa-p/automation-questions"), //  Automation
        new URI("https://community.atlassian.com/t5/Jira-Service-Management/qa-p/jira-service-desk-questions"), //  JSM
        new URI("https://community.atlassian.com/t5/Bitbucket-questions/qa-p/bitbucket-questions"),  //  Bitbucket
        new URI("https://community.atlassian.com/t5/Atlassian-Platform-questions/qa-p/atlassian-platform-questions"), //  Atlassian Platform
        new URI("https://community.atlassian.com/t5/Atlassian-Analytics-questions/qa-p/analytics-questions"), //  Atlassian Analytics
        new URI("https://community.atlassian.com/t5/Sourcetree-questions/qa-p/sourcetree-questions"), //  Source Tree
        new URI("https://community.atlassian.com/t5/Announcement-questions/qa-p/announcement-questions")  //  Community Announcements
    );
  }

  @Bean
  @EntryPoint.Stackoverflow
  Flux<URI> stackoverflowEntryPoint() throws URISyntaxException {
    return Flux.just(
        new URI("https://stackoverflow.com/questions/tagged/confluence"),
        new URI("https://stackoverflow.com/questions/tagged/confluence-rest-api"),
        new URI("https://stackoverflow.com/questions/tagged/jira"),
        new URI("https://stackoverflow.com/questions/tagged/jira-rest-api"),
        new URI("https://stackoverflow.com/questions/tagged/jira-plugin"),
        new URI("https://stackoverflow.com/questions/tagged/jira-agile"),
        new URI("https://stackoverflow.com/questions/tagged/scriptrunner-for-jira"),
        new URI("https://stackoverflow.com/questions/tagged/bitbucket"),
        new URI("https://stackoverflow.com/questions/tagged/jql"),
        new URI("https://stackoverflow.com/questions/tagged/atlassian-sourcetree"),
        new URI("https://stackoverflow.com/questions/tagged/bamboo"),
        new URI("https://stackoverflow.com/questions/tagged/atlassian-fisheye"),
        new URI("https://stackoverflow.com/questions/tagged/statuspage-api"),
        new URI("https://stackoverflow.com/questions/tagged/atlassian-crowd"),
        new URI("https://stackoverflow.com/questions/tagged/opsgenie"),
        new URI("https://stackoverflow.com/questions/tagged/bitbucket-pipelines"),
        new URI("https://stackoverflow.com/questions/tagged/trello"),
        new URI("https://stackoverflow.com/questions/tagged/jira-zephyr")
    );
  }

}
class ScrapeCondition implements Condition {
  private final String SCRAPE_ARGS = "scrape";

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    ApplicationArguments args = context.getBeanFactory().getBean(ApplicationArguments.class);
    return args.containsOption(SCRAPE_ARGS) || args.getNonOptionArgs().contains(SCRAPE_ARGS);
  }
}