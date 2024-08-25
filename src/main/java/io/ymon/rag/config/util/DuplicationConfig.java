package io.ymon.rag.config.util;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DuplicationConfig {

  private final Set<URIWrapper> visitedUrlSet = ConcurrentHashMap.newKeySet();

  @Bean
  Predicate<URI> duplicationChecker() {
    return url -> visitedUrlSet.add(new URIWrapper(url));
  }

  private final static class URIWrapper {
    private final String host;
    private final String path;
    private final String query;
    private final int port;

    public URIWrapper(URI url) {
      this.host = url.getHost();
      this.path = url.getPath() != null ? url.getPath() : "";
      this.query = url.getQuery() != null ? url.getQuery() : "";
      this.port = url.getPort();
    }

    @Override
    public int hashCode() {
      int result = host.hashCode();

      result = result * 31 + path.hashCode();
      result = result * 31 + query.hashCode();
      result = result * 31 + port;
      return result;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o instanceof URIWrapper t)
        return
            host.equals(t.host) &&
                path.equals(t.path) &&
                query.equals(t.query) &&
                port == t.port;
      return false;
    }
  }

}
