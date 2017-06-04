package ua.devchallenge.service;

import java.util.function.Supplier;

import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rx.Completable;
import rx.subjects.PublishSubject;
import ua.devchallenge.domain.Article;
import ua.devchallenge.domain.Link;
import ua.devchallenge.domain.Website;
import ua.devchallenge.persistence.ArticleRepository;
import ua.devchallenge.persistence.WebsiteRepository;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollector {

    private final ArticleRepository articleRepository;
    private final WebsiteRepository websiteRepository;
    private final WebClient webClient;

    public Completable collect(Link link, Supplier<Long> scoreSupplier) {
        PublishSubject<Object> publishSubject = PublishSubject.create();
        Website website = link.getWebsite();
        webClient.requestAbs(HttpMethod.GET, link.getUrl())
            .send(ar -> {
                if (ar.succeeded()) {
                    String body = ar.result().bodyAsString();
                    Article article = new Article(link.getUrl(), body);
                    articleRepository.save(article)
                        .andThen(websiteRepository.updateArticleSet(website, article, scoreSupplier.get()))
                        .subscribe(publishSubject::onCompleted);
                } else {
                    log.error("Failed to fetch {}: {}", link, getStackTrace(ar.cause()));
                    publishSubject.onError(ar.cause());
                }
            });
        return publishSubject.toCompletable();
    }
}
