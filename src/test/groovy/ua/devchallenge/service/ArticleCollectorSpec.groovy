package ua.devchallenge.service

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.rxjava.ext.web.client.HttpRequest
import io.vertx.rxjava.ext.web.client.HttpResponse
import io.vertx.rxjava.ext.web.client.WebClient
import rx.observers.TestSubscriber
import spock.lang.Specification
import ua.devchallenge.domain.Article
import ua.devchallenge.domain.Link
import ua.devchallenge.domain.Website
import ua.devchallenge.persistence.ArticleRepository
import ua.devchallenge.persistence.WebsiteRepository

import java.util.function.Supplier

import static io.vertx.core.Future.failedFuture
import static io.vertx.core.Future.succeededFuture
import static rx.Completable.complete
import static ua.devchallenge.fixture.RxFixture.awaitCompleted
import static ua.devchallenge.fixture.RxFixture.awaitError

class ArticleCollectorSpec extends Specification {

    ArticleRepository articleRepository = Mock()
    WebsiteRepository websiteRepository = Mock()
    WebClient webClient = Mock()

    def target = new ArticleCollector(articleRepository, websiteRepository, webClient)

    def websiteUrl = UUID.randomUUID().toString()

    def website = Website.builder()
            .url(websiteUrl)
            .prevButtonXPath("//li[contains(@class, 'prev')]/a")
            .nextButtonXPath("//li[contains(@class, 'next')]/a")
            .linkXpath("//h2[contains(@class, 'post-title')]/a")
            .maxScanDepth(100)
            .latestPercent(0.1)
            .build()


    def "should collect link"() {
        given:
        def articleUrl = "a"
        def articleBody = "body"
        def ts = 1L
        Article article = new Article(articleUrl, articleBody)

        def subscriber = TestSubscriber.create()
        HttpRequest httpRequest = Mock()
        HttpResponse httpResponse = Mock()
        Supplier scoreSupplier = Mock()

        when:
        target.collect(new Link(website, articleUrl), scoreSupplier).subscribe(subscriber)

        then:
        awaitCompleted(subscriber)

        and:
        1 * webClient.requestAbs(HttpMethod.GET, articleUrl) >> httpRequest
        1 * httpRequest.send(_) >> { Handler handler ->
            handler.handle(succeededFuture(httpResponse))
        }
        1 * httpResponse.bodyAsString() >> articleBody
        1 * articleRepository.save(article) >> complete()
        1 * websiteRepository.updateArticleSet(website, article, ts) >> complete()
        1 * scoreSupplier.get() >> ts
        0 * _
    }

    def "should fail if failed to collect link"() {
        given:
        def articleUrl = "a"

        def subscriber = TestSubscriber.create()
        HttpRequest httpRequest = Mock()
        Supplier scoreSupplier = Mock()

        when:
        target.collect(new Link(website, articleUrl), scoreSupplier).subscribe(subscriber)

        then:
        awaitError(subscriber, RuntimeException)

        and:
        1 * webClient.requestAbs(HttpMethod.GET, articleUrl) >> httpRequest
        1 * httpRequest.send(_) >> { Handler handler ->
            handler.handle(failedFuture(new RuntimeException()))
        }
        0 * _
    }
}
