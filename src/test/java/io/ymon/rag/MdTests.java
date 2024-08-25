package io.ymon.rag;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MdTests {
  private final FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();

  //@Test
  void titleTest() {
    String html = "<h1>H1</h1>";
    String md = converter.convert(html);

    String expected = "# H1\n";
    assertThat(md).isEqualTo(expected);
  }

  @Test
  void bodyTest() {
    String html = "<p>hi, guys</p><p>thanks</p>";
    String md = converter.convert(html);

    String expected = "hi, guys\n\nthanks\n";
    assertThat(md).isEqualTo(expected);
    System.out.println(md);
  }

  @Test
  void unclosedTest() {
    String html = "<p>wrong<p>wrong2</p>";
    String md = converter.convert(html);

    String expected1 = "wrong\n\nwrong2\n";
    String expected2 = "";
    assertThat(md).contains(expected1, expected2);
    System.out.println(md);
  }

  //@Test
  void convertMd() {
    String html = "<p>hi, guys</p><p>thanks</p>";
    String md = converter.convert(html);
    String md2 = converter.convert(md);

    assertThat(md).isEqualTo(md2);
    System.out.println(md);
    System.out.println(md2);
  }
}
