package ua.devchallenge.exception;

public class WebsiteDoesNotExistException extends RuntimeException {
    public WebsiteDoesNotExistException(String url) {
        super(String.format("Config for website %s does not exist", url));
    }
}
