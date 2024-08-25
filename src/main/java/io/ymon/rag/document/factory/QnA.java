package io.ymon.rag.document.factory;

import java.util.List;

public record QnA(Question question, List<Answer> answers, List<String> tags, String url) {

  public record Question(String author, String title, String body, List<Comment> comments) {}

  public record Answer(String author, String body, List<Comment> comments) {}

  public record Comment(String author, String body) {}
}