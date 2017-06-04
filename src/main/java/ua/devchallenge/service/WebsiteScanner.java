package ua.devchallenge.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rx.Completable;
import rx.Observable;
import ua.devchallenge.domain.Link;
import ua.devchallenge.domain.Website;
import ua.devchallenge.domain.dto.ScanEvent;
import ua.devchallenge.persistence.ArticleRepository;
import ua.devchallenge.persistence.WebsiteRepository;

import static rx.Observable.from;
import static rx.Single.just;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebsiteScanner {

    private final LinkScrapper linkScrapper;
    private final ArticleRepository articleRepository;
    private final WebsiteRepository websiteRepository;
    private final ArticleCollector articleCollector;
    private final Supplier<Long> currentMillis;

    /**
     * Scans all new articles + percent of existing articles, and removes deleted articles
     *
     * @param event trigger for scan
     */
    public Completable apply(ScanEvent event) {
        Website website = event.getWebsite();
        return websiteRepository.getLatestArticles(website, event.getExistingScanDepth())
            .toList()
            .flatMap(existingArticles -> {
                Observable<LinkMeta> links = linkScrapper.links(website)
                    .concatMap(link -> articleRepository.get(link.getUrl())
                        .map(article -> new LinkMeta(link, true))
                        .onErrorResumeNext(just(new LinkMeta(link, false)))
                        .toObservable()
                    );

                Supplier<Long> scoreSupplier = new AtomicLong(currentMillis.get() + event.getExistingScanDepth())::getAndDecrement;
                if (existingArticles.isEmpty()) {
                    links = links.take(event.getExistingScanDepth());
                } else {
                    AtomicInteger visitedCounter = new AtomicInteger();
                    links = links
                        .takeUntil(linkMeta -> {
                            if (linkMeta.isVisited()) {
                                visitedCounter.incrementAndGet();
                            }
                            return visitedCounter.get() >= event.getExistingScanDepth();
                        });
                }
                return links
                    .map(LinkMeta::getLink)
                    .toList()
                    .flatMap(newLinks -> updateLatestArticles(website, newLinks, existingArticles, scoreSupplier).toObservable());
            })
            .toCompletable();
    }

    private Completable updateLatestArticles(Website website, List<Link> newLinks, List<String> removalCandidates, Supplier<Long> scoreSupplier) {
        Set<String> newArticleSet = newLinks.stream()
            .map(Link::getUrl)
            .collect(Collectors.toSet());
        removalCandidates.removeIf(newArticleSet::contains);

        Completable removeDeletedArticles = from(removalCandidates)
            .flatMap(url -> articleRepository.remove(url).toObservable())
            .toCompletable();

        Completable updateLatestArticles = from(newLinks)
            .concatMap(link -> articleCollector.collect(link, scoreSupplier).toObservable())
            .toCompletable();

        return removeDeletedArticles.andThen(updateLatestArticles)
            .doOnCompleted(() -> log.info("Updated latest articles for {}", website));
    }

    @Value
    private static class LinkMeta {
        Link link;
        boolean visited;
    }

}
