package ua.devchallenge.persistence;

import io.vertx.core.json.JsonArray;
import io.vertx.redis.op.RangeOptions;
import io.vertx.rxjava.redis.RedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Func1;
import ua.devchallenge.domain.Article;
import ua.devchallenge.domain.Website;
import ua.devchallenge.exception.WebsiteDoesNotExistException;

import static io.vertx.core.json.Json.decodeValue;
import static io.vertx.core.json.Json.encode;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebsiteRepository {

    private static final String WEBSITE_NAMESPACE = "website";

    private final RedisClient redisClient;

    public Single<Website> get(String url) {
        return redisClient.rxGet(url)
            .map(response -> {
                if (response == null) {
                    throw new WebsiteDoesNotExistException(url);
                }
                return decodeValue(response, Website.class);
            });
    }

    @SuppressWarnings("unchecked")
    public Observable<String> all() {
        return redisClient.rxSmembers(WEBSITE_NAMESPACE)
            .toObservable()
            .flatMapIterable(JsonArray::getList);
    }

    public Completable save(Website website) {
        return redisClient.rxSet(website.getUrl(), encode(website))
            .flatMap($ -> redisClient.rxSadd(WEBSITE_NAMESPACE, website.getUrl()))
            .toCompletable();
    }

    public Completable updateArticleSet(Website website, Article article, long updateTs) {
        return redisClient.rxZadd(articlesKey(website.getUrl()), updateTs, article.getUrl())
            .doOnSuccess(i -> log.info("Updated website: {} set with: {}", website, article.getUrl()))
            .toCompletable();
    }

    public Observable<String> getArticles(String url) {
        return redisClient.rxZrange(articlesKey(url), 0, -1)
            .toObservable()
            .flatMapIterable((Func1<JsonArray, Iterable<? extends String>>) JsonArray::getList);
    }

    public Observable<String> getLatestArticles(Website website, long count) {
        return redisClient.rxZrevrange(articlesKey(website.getUrl()), 0, count, RangeOptions.NONE)
            .toObservable()
            .flatMapIterable((Func1<JsonArray, Iterable<? extends String>>) JsonArray::getList);
    }

    private String articlesKey(String url) {
        return WEBSITE_NAMESPACE + "::" + url;
    }

}
