package io.ymon.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ymon.rag.document.factory.QnA;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FilterTests {

  private final Predicate<URI> dupChecker;

  @Autowired
  FilterTests(Predicate<URI> dupChecker) {
    this.dupChecker = dupChecker;
  }

  @Test
  void duplicationTest() throws URISyntaxException {
    URI url = new URI("https://www.a.b.c/1");

    List<URI> shouldBeFalse = List.of(
        new URI("https://www.a.b.c/1"),
        new URI("http://www.a.b.c/1"),
        new URI("https://www.a.b.c/1#1"),
        new URI("https://www.a.b.c/1")
    );

    List<URI> shouldBeTrue = List.of(
        new URI("https://www.a.b.c/1?q=w"),
        new URI("https://www.a.b.c:234/1"),
        new URI("https://www.a.b.c"),
        new URI("https://blog.a.b.c/1"),
        new URI("https://www.a.b.c/1/2"),
        new URI("https://www.a.b.c/1/")
    );

    //  add Visited Set -> Should Be True
    assertThat(dupChecker.test(url)).isTrue();

    //  Same Test
    shouldBeFalse.forEach(sameUrl -> assertThat(dupChecker.test(sameUrl))
        .as("Expected %s is %s", sameUrl, url).isFalse());

    //  Diff Test
    shouldBeTrue.forEach(diffUrl -> assertThat(dupChecker.test(diffUrl))
        .as("Expected %s is not %s", diffUrl, url).isTrue());
  }

  @Test
  void duplicationTest2() throws URISyntaxException {
    URI url = new URI("https://community.atlassian.com/t5/Confluence-questions/qa-p/confluence-questions/page/3");

    List<URI> shouldBeFalse = List.of(
        new URI("https://community.atlassian.com/t5/Confluence-questions/qa-p/confluence-questions/page/3"),
        new URI("https://community.atlassian.com/t5/Confluence-questions/qa-p/confluence-questions/page/3"),
        new URI("https://community.atlassian.com/t5/Confluence-questions/qa-p/confluence-questions/page/3")
    );

    List<URI> shouldBeTrue = List.of(
        new URI("https://community.atlassian.com/t5/Confluence-questions/qa-p/confluence-questions/page/5"),
        new URI("https://community.atlassian.com/t5/Jira-questions/qa-p/jira-questions/page/2")
    );

    //  add Visited Set -> Should Be True
    assertThat(dupChecker.test(url)).isTrue();

    //  Same Test
    shouldBeFalse.forEach(sameUrl -> assertThat(dupChecker.test(sameUrl))
        .as("Expected %s is %s", sameUrl, url).isFalse());

    //  Diff Test
    shouldBeTrue.forEach(diffUrl -> assertThat(dupChecker.test(diffUrl))
        .as("Expected %s is not %s", diffUrl, url).isTrue());
  }


  @Test
  void qnaDeserialize() throws JsonProcessingException {
    String stringQna = "{\"question\":{\"author\":\"Author\",\"title\":\"Title\",\"body\":\"Question Body\"},\"answers\":[{\"author\":\"Author\",\"body\":\"Answer\",\"comments\":[{\"author\":\"Author0\",\"body\":\"Comment0\"},{\"author\":\"Author1\",\"body\":\"Comment1\"}]}],\"tags\":[],\"url\":\"\"}\n";

    ObjectMapper om = new ObjectMapper();

    QnA qna = om.readValue(stringQna, QnA.class);

    System.out.println(qna);
  }
}
