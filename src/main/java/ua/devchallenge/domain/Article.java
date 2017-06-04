package ua.devchallenge.domain;

import lombok.Value;

@Value
public class Article {
    String url;
    String body;

    public boolean hasSameContent(Article other) {
        return this.body.equals(other.getBody());
    }
}
