package io.ymon.rag;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RetryException extends RuntimeException {
  public final static long DEFAULT_RETRY_AFTER_MILLIS = 3_000;
  private final long retryAfterMillis;

  public RetryException() {
    this(DEFAULT_RETRY_AFTER_MILLIS);
  }

  public RetryException(long retryAfterMillis) {
    super();
    this.retryAfterMillis = retryAfterMillis;
  }

  public RetryException(String message) {
    this(message, DEFAULT_RETRY_AFTER_MILLIS);
  }

  public RetryException(String message, long retryAfterMillis) {
    super(message);
    this.retryAfterMillis = retryAfterMillis;
  }
  public static long parseRetryAfter(String retryAfter) {
    if (retryAfter == null) return DEFAULT_RETRY_AFTER_MILLIS;

    try {
      Long res = Long.parseLong(retryAfter);

      return res * 1000;  // 초 단위
    } catch (NumberFormatException e) {
      try {
        ZonedDateTime dateTime = ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME);
        return Duration.between(ZonedDateTime.now(), dateTime).toMillis();
      } catch (Exception ignored) {
        return DEFAULT_RETRY_AFTER_MILLIS;
      }
    }
  }
  public long getRetryAfterMillis() { return retryAfterMillis; }
}
