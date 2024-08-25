package io.ymon.rag.document.factory;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import io.ymon.rag.document.factory.QnA.Answer;
import io.ymon.rag.document.factory.QnA.Comment;
import io.ymon.rag.document.factory.QnA.Question;
import java.util.List;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class AtlassianQnAFactory implements QnAFactory {
  private final static FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();

  @Override
  public QnA create(Element element) {
    if (element == null) return null;
    Question question = question(
        element.getElementsByClass("lia-message-view-qanda-question").first());
    List<Answer> answers = answers(
        element.select(".atl-qanda-answers-listing .atl-thread-listing__message-wrapper"));
    List<String> tags = element.select("ul.atl-tags-list li a").stream().map(Element::text)
        .map(String::trim).toList();
    String url = element.baseUri();
    if (answers == null || answers.isEmpty()) return null;
    return new QnA(question, answers, tags, url);
  }

  private Question question(Element element) {
    if (element == null) return null;
    String author = element.getElementsByClass("atl-author-url").first().text().trim();
    String title = element.getElementsByClass("atl-page-title").first().text().trim();
    String body = converter.convert(element.selectFirst(".lia-quilt-qanda-question .lia-message-body-content"));
    return new Question(author, title, body, null);
  }

  private List<Answer> answers(Elements elements) {
    if (elements == null) return null;
    return elements.stream().map(this::answer).toList();
  }

  private Answer answer(Element element) {
    if (element == null) return null;
    String author = element.getElementsByClass("atl-author-url").first().text().trim();
    String body = converter.convert(element.getElementsByClass("lia-message-body").first());
    List<Comment> comments = comments(element.select(".atl-thread-listing__message-comment"));
    return new Answer(author, body, comments);
  }

  private List<Comment> comments(Elements elements) {
    return elements.stream().map(this::comment).toList();
  }

  private Comment comment(Element element) {
    String author = element.getElementsByClass("atl-author-url").first().text().trim();
    String body = converter.convert(element.getElementsByClass("lia-message-body").first());
    return new Comment(author, body);
  }

}