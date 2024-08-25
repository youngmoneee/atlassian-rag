package io.ymon.rag.config.util;

import io.ymon.rag.RetryException;
import io.ymon.rag.document.Transformer;
import java.net.URI;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Slf4j
@Configuration
public class WebClientConfig {

  private final Predicate<Throwable>
      isNettyException = t -> t instanceof WebClientException;

  @Bean
  RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

  @Bean
  public WebClient client(HttpClient httpClient) {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(conf -> conf.defaultCodecs().maxInMemorySize(-1))
        .build();
  }

  @Bean
  Transformer<URI, Element> fetchingWithFilter(Predicate<URI> filter, Function<URI, Mono<Element>> fetching) {
    return urlFlux -> urlFlux.filter(filter)
        .delayElements(Duration.ofMillis(10))
        .flatMap(fetching, 30);
  }

  @Bean
  Function<URI, Mono<Element>> fetching(WebClient webClient) {
    return url -> webClient
        //  Request
        .get().uri(url)
        .exchangeToMono(this::responseFilter) //  2xx
        .onErrorMap(isNettyException, e -> new RetryException(e.getMessage()))  //  의존성

        //  Retry on Error
        .retryWhen(retryStrategy())
        .doOnError(t -> log.error("Fetching Error : {}", t.getMessage()))
        .onErrorResume(t -> Mono.empty())

        //  convert To Document
        .zipWith(
            Mono.just(url.toString()),
            Jsoup::parse
        ).cast(Element.class) //  Todo : Alias interface?

        //  catch Parse Error
        .doOnError(t -> log.error("Parsing Error : {}", url))
        .onErrorResume(t -> Mono.empty());
  }

  /**
   * private Area
   */

  private Retry retryStrategy() {
    return Retry.from(signalFlux -> signalFlux
        .flatMap(signal -> {
          if (signal.failure() instanceof RetryException retryContext)
            return Mono.delay(Duration.ofMillis(retryContext.getRetryAfterMillis()));
          return Mono.error(signal.failure());  //  RetryException 아니면 그대로 방출
        }).take(3)
    );
  }

  private Mono<String> responseFilter(ClientResponse r) {
    if (r.statusCode().is2xxSuccessful()) return r.bodyToFlux(String.class)
        //  TODO : String.class -> DataBuffer.class
        .reduceWith(StringBuilder::new, StringBuilder::append)
        .map(StringBuilder::toString)
        .doOnError(t -> log.error("Request Error : {}", t.getMessage()))
        .onErrorResume(t -> Mono.error(new RetryException(t.getMessage())));
    else {
      long retryAfterMillis = RetryException  //  Retry-After 헤더 있으면 이후 재시도
          .parseRetryAfter(r.headers().asHttpHeaders().getFirst("Retry-After"));

      return Mono.error(new RetryException(r.statusCode().toString(), retryAfterMillis));
    }
  }
}