package ua.devchallenge.domain;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
public class Page {

    Website website;
    String currentUrl;
    List<Link> links;
    Link nextLink;
    Link prevLink;

    @SneakyThrows
    public static Page from(WebClient webClient, Website website, String url) {

        log.info("GET: {}", url);

        HtmlPage page = webClient.getPage(url);

        List<Link> links = page.getByXPath(website.getLinkXpath()).stream()
            .map(HtmlElement.class::cast)
            .map(link -> link.getAttribute("href"))
            .map(href -> new Link(website, href))
            .collect(Collectors.toList());

        HtmlElement nextLink = page.getFirstByXPath(website.getNextButtonXPath());
        HtmlElement prevLink = page.getFirstByXPath(website.getPrevButtonXPath());

        String nextHref = nextLink != null ? nextLink.getAttribute("href") : null;
        String prevHref = prevLink != null ? prevLink.getAttribute("href") : null;

        nextHref = absoluteUrl(url, nextHref);
        prevHref = absoluteUrl(url, prevHref);

        return new Page(website, url, links, new Link(website, nextHref), new Link(website, prevHref));
    }

    public Page next(WebClient webClient) {
        return from(webClient, website, nextLink.getUrl());
    }

    public Page prev(WebClient webClient) {
        return from(webClient, website, prevLink.getUrl());
    }

    public boolean hasNext() {
        return nextLink != null;
    }

    public boolean hasPrev() {
        return prevLink != null;
    }

    @SneakyThrows
    private static String absoluteUrl(String currentUrl, String uri) {
        if (uri == null) {
            return null;
        }
        URL baseUrl = new URL(currentUrl);
        URL url = new URL(baseUrl, uri);
        return url.toExternalForm();
    }
}
