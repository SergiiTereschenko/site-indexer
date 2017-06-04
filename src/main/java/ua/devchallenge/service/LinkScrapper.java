package ua.devchallenge.service;

import com.gargoylesoftware.htmlunit.WebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rx.Observable;
import ua.devchallenge.domain.Link;
import ua.devchallenge.domain.Page;
import ua.devchallenge.domain.Website;

import static rx.Observable.defer;
import static rx.Observable.empty;
import static rx.Observable.just;

@Service
@RequiredArgsConstructor
public class LinkScrapper {

    private final WebClient webClient;

    public Observable<Link> links(Website website) {
        Page firstPage = Page.from(webClient, website, website.getUrl());
        return links(firstPage);
    }

    private Observable<Link> links(Page page) {
        return defer(() -> just(page).flatMapIterable(Page::getLinks))
            .concatWith(defer(
                () -> {
                    if (page.hasNext()) {
                        return links(page.next(webClient));
                    } else {
                        return empty();
                    }
                })
            );
    }
}
