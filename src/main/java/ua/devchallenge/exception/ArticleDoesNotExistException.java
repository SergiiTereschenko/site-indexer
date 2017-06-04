package ua.devchallenge.exception;

public class ArticleDoesNotExistException extends RuntimeException {
    public ArticleDoesNotExistException(String url) {
        super(String.format("Article %s does not exist", url));
    }
}
