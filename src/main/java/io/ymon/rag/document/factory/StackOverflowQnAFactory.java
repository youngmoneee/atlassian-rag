package io.ymon.rag.document.factory;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import io.ymon.rag.document.factory.QnA.Answer;
import io.ymon.rag.document.factory.QnA.Comment;
import io.ymon.rag.document.factory.QnA.Question;
import java.util.List;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class StackOverflowQnAFactory implements QnAFactory {
  private final static FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();

  @Override
  public QnA create(Element element) {
    if (element == null) return null;
    element.getElementsByTag("aside").remove();
    String title = element.getElementsByTag("h1").first().text();
    Question question = question(title, element.getElementById("question"));
    List<Answer> answers = answers(element.getElementById("answers"));
    List<String> tags = element.getElementById("question")
        .getElementsByAttributeValueContaining("rel", "tag").stream()
        .map(Element::text).map(String::trim).toList();
    String url = element.baseUri();
    if (answers.isEmpty() && (question.comments().isEmpty())) return null;
    return new QnA(question, answers, tags, url);
  }

  private Question question(String title, Element element) {
    if (element == null) return null;
    String author = element.selectFirst("[itemprop=name]").text();
    String body = converter.convert(element.getElementsByClass("js-post-body").first());
    List<Comment> comments = comments(element.getElementsByClass("comment-body"));
    return new Question(author, title, body, comments);
  }

  private List<Answer> answers(Element element) {
    if (element == null) return null;
    return element.getElementsByClass("answer").stream().map(this::answer).toList();
  }

  private Answer answer(Element element) {
    if (element == null) return null;
    String author = element.selectFirst("[itemprop=name]").text();
    String body = converter.convert(element.getElementsByClass("js-post-body").first());
    List<Comment> comments = comments(element.getElementsByClass("comment-body"));
    return new Answer(author, body, comments);
  }

  private List<Comment> comments(Elements elements) {
    return elements.stream().map(this::comment).toList();
  }

  private Comment comment(Element element) {
    String author = element.getElementsByClass("comment-user").first().text().trim();
    String  body = converter.convert(element.getElementsByClass("comment-copy").first());
    return new Comment(author, body);
  }
}
