package io.ymon.rag;

public enum DataType {
  URL("url"),
  QnA("qna"),
  DOCUMENT("document"),
  QUESTION("question"),
  ANSWERS("answers"),
  TAGS("tags"),
  JSOUP_DOCUMENT("jsoup-document"),
  SPRING_AI_DOCUMENT("ai-document"),
  UNHANDLED("unhandled");

  private final String value;

  DataType(String value) { this.value = value; }

  public String getValue() { return value; }

  @Override
  public String toString() { return getValue(); }
}
