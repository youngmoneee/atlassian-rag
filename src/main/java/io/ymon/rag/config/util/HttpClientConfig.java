package io.ymon.rag.config.util;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorResourceFactory;
import reactor.netty.http.HttpResources;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.SslProvider.Builder;
import reactor.netty.tcp.SslProvider.SslContextSpec;

@Configuration
public class HttpClientConfig {
  private static final int SSL_TIMEOUT_MILLIS = 30_000;
  private static final int ACQUIRE_TIMEOUT_MILLIS = 30_000;
  private static final int CONNECT_TIMEOUT_MILLIS = SSL_TIMEOUT_MILLIS * 2;
  private static final int READ_TIMEOUT_MILLIS = 60_000;
  private static final int HTTP_MAX_HEADER_SIZE = 1024 * 8;

  @Bean
  ReactorResourceFactory reactorResourceFactory(ConnectionProvider cp) {
    ReactorResourceFactory factory = new ReactorResourceFactory();

    factory.setUseGlobalResources(false);
    factory.setConnectionProvider(cp);
    factory.setLoopResources(HttpResources.get());
    return factory;
  }

  @Bean
  HttpClient httpClient(ConnectionProvider connectionProvider) {
    return HttpClient.create(connectionProvider)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
        .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)))
        .httpResponseDecoder(spec -> spec.maxHeaderSize(HTTP_MAX_HEADER_SIZE))
        .secure(SslProviderBuilder::byDefault)
        .followRedirect(false)
        .resolver(DefaultAddressResolverGroup.INSTANCE);
  }

  @Bean
  ConnectionProvider connectionProvider() {
    return ConnectionProvider.builder("connection-pool")
        .maxConnections(256)
        .pendingAcquireMaxCount(512)
        .pendingAcquireTimeout(Duration.ofMillis(ACQUIRE_TIMEOUT_MILLIS))
        .evictInBackground(Duration.ofSeconds(30))
        .disposeTimeout(Duration.ofSeconds(60))
        .build();
  }

  //  private

  private static class SslProviderBuilder {
    static Builder byDefault(SslContextSpec spec) {
      return spec.sslContext(defaultSslContext())
          .handshakeTimeout(Duration.ofMillis(SSL_TIMEOUT_MILLIS));
    }

    private static SslContext defaultSslContext() {
      try {
        return SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
      } catch (SSLException e) {
        return null;
      }
    }
  }
}
