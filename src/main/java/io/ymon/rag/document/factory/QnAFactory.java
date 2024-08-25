package io.ymon.rag.document.factory;

import java.util.function.Function;
import org.jsoup.nodes.Element;

@FunctionalInterface
public interface QnAFactory extends Function<Element, QnA> {
  QnA create(Element doc);

  @Override
  default QnA apply(Element element) {
    try {
      return create(element);
    } catch (Exception e) {
      return null;
    }
  }
}