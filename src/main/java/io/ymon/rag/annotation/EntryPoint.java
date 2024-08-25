package io.ymon.rag.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 처음에 탐색할 URL List
 */
@Target({
    ElementType.PARAMETER,
    ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface EntryPoint {
  String value() default "entryPointUrl";

  @Target({
      ElementType.PARAMETER,
      ElementType.METHOD
  })
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Atlassian {
    String value() default "atlassian";
  }

  @Target({
      ElementType.PARAMETER,
      ElementType.METHOD
  })
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Stackoverflow {
    String value() default "stackoverflow";
  }
}