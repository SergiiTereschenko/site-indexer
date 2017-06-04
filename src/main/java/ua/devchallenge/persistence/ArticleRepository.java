package ua.devchallenge.persistence;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import difflib.DiffUtils;
import difflib.Patch;
import io.vertx.rxjava.redis.RedisClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import rx.Completable;
import rx.Single;
import ua.devchallenge.domain.Article;
import ua.devchallenge.exception.ArticleDoesNotExistException;
import ua.devchallenge.utils.Json;

import static io.vertx.core.json.Json.decodeValue;
import static io.vertx.core.json.Json.encode;
import static java.util.stream.Collectors.joining;
import static rx.Completable.complete;
import static rx.Completable.error;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleRepository {

    private static final String REVISION_NAMESPACE = "revision";

    private final RedisClient redisClient;

    public Completable save(Article article) {
        return get(getKey(article))
            .flatMapCompletable(existingArticle -> {
                if (existingArticle.hasSameContent(article)) {
                    return complete();
                } else {
                    return appendRevision(existingArticle, article)
                        .andThen(set(article));
                }
            })
            .onErrorResumeNext(e -> {
                if (e instanceof ArticleDoesNotExistException) {
                    return set(article);
                }
                return error(e);
            });
    }

    public Single<Article> get(String url) {
        return redisClient.rxGet(url)
            .map(response -> {
                if (response == null) {
                    throw new ArticleDoesNotExistException(url);
                }
                return decodeValue(response, Article.class);
            });
    }

    public Completable remove(String url) {
        Article newArticle = new Article(url, "");
        return get(url)
            .flatMapCompletable(article -> appendRevision(article, newArticle))
            .andThen(set(newArticle));
    }

    @SuppressWarnings("unchecked")
    public Single<Article> getRevision(String url, int version) {
        return get(url)
            .flatMap(article -> redisClient.rxLrange(getRevisionKey(url), version, -1)
                .map(response -> {
                        List<String> mostRecentArticleBody = lines(article.getBody());
                        ArrayList<Object> revisions = Lists.newArrayList(response);
                        Collections.reverse(revisions);
                        for (Object revision : revisions) {
                            String value = (String) revision;
                            Patch<String> patch = (Patch<String>) Json.decodeValue(value, Patch.class);
                            mostRecentArticleBody = applyPatch(mostRecentArticleBody, patch);
                        }
                        return new Article(article.getUrl(), mostRecentArticleBody.stream().collect(joining("\n")));
                    }
                )
            );
    }

    @SuppressWarnings("unchecked")
    public Single<Patch<String>> getDiff(String url, int v1, int v2) {
        return get(url)
            .flatMap(article -> redisClient.rxLrange(getRevisionKey(url), v1, -1)
                .map(response -> {
                        List<String> v2Article = lines(article.getBody());
                        ArrayList<Object> revisions = Lists.newArrayList(response);
                        int currentRevision = response.size() - 1;

                        // create v2
                        while (currentRevision >= v2 - v1) {
                            String value = (String) revisions.get(currentRevision);
                            Patch<String> patch = (Patch<String>) Json.decodeValue(value, Patch.class);
                            v2Article = applyPatch(v2Article, patch);
                            currentRevision--;
                        }

                        // create v1
                        List<String> v1Article = Lists.newArrayList(v2Article);
                        while (currentRevision >= 0) {
                            String value = (String) revisions.get(currentRevision);
                            Patch<String> patch = (Patch<String>) Json.decodeValue(value, Patch.class);
                            v1Article = applyPatch(v1Article, patch);
                            currentRevision--;
                        }

                        return DiffUtils.diff(v1Article, v2Article);
                    }
                )
            );
    }

    private Completable appendRevision(Article existingArticle, Article newArticle) {
        String key = getRevisionKey(newArticle.getUrl());
        List<String> existingArticleBody = lines(existingArticle.getBody());
        List<String> newArticleBody = lines(newArticle.getBody());
        Patch<String> patch = DiffUtils.diff(newArticleBody, existingArticleBody);

        return redisClient.rxRpush(key, encode(patch))
            .doOnSuccess(i -> log.info("Created new revision {} with {}", newArticle.getUrl(), patch))
            .toCompletable();
    }

    private Completable set(Article article) {
        return redisClient.rxSet(getKey(article), encode(article))
            .doOnSuccess(i -> log.info("Upserted article: {}", article.getUrl()))
            .toCompletable();
    }

    private String getKey(Article article) {
        return article.getUrl();
    }

    private String getRevisionKey(String url) {
        return REVISION_NAMESPACE + "::" + url;
    }

    @SneakyThrows
    private static List<String> lines(String body) {
        return IOUtils.readLines(new StringReader(body));
    }

    @SneakyThrows
    private List<String> applyPatch(List<String> mostRecentArticleBody, Patch<String> patch) {
        return patch.applyTo(mostRecentArticleBody);
    }
}
