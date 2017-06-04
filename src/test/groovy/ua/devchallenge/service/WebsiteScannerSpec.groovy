package ua.devchallenge.service

import rx.Completable
import rx.Observable
import rx.Single
import rx.observers.TestSubscriber
import spock.lang.Specification
import ua.devchallenge.domain.Article
import ua.devchallenge.domain.Link
import ua.devchallenge.domain.Website
import ua.devchallenge.domain.dto.ScanEvent
import ua.devchallenge.persistence.ArticleRepository
import ua.devchallenge.persistence.WebsiteRepository

import java.util.function.Supplier

import static rx.Observable.just
import static ua.devchallenge.fixture.RxFixture.awaitCompleted

class WebsiteScannerSpec extends Specification {
    LinkScrapper linkScrapper = Mock()
    ArticleRepository articleRepository = Mock()
    WebsiteRepository websiteRepository = Mock()
    ArticleCollector articleCollector = Mock()
    Supplier<Long> currentMillis = Mock()

    def target = new WebsiteScanner(linkScrapper, articleRepository, websiteRepository, articleCollector, currentMillis)

    def website = Website.builder()
            .url("google.com")
            .prevButtonXPath("//li[contains(@class, 'prev')]/a")
            .nextButtonXPath("//li[contains(@class, 'next')]/a")
            .linkXpath("//h2[contains(@class, 'post-title')]/a")
            .maxScanDepth(100)
            .latestPercent(0.1)
            .build()

    def "should scan initial articles"() {
        given:
        def testSubscriber = TestSubscriber.create()
        def depth = 10
        def ts = 234234L

        when:
        target.apply(new ScanEvent(website, depth)).subscribe(testSubscriber)

        then:
        1 * websiteRepository.getLatestArticles(website, depth) >> Observable.empty()
        1 * linkScrapper.links(website) >> just(new Link(website, "c"), new Link(website, "a"))
        1 * articleRepository.get("c") >> Single.error(new RuntimeException())
        1 * articleRepository.get("a") >> Single.error(new RuntimeException())
        1 * currentMillis.get() >> ts
        1 * articleCollector.collect(new Link(website, "c"), _) >> Completable.complete()
        1 * articleCollector.collect(new Link(website, "a"), _) >> { _, Supplier supplier ->
            assert supplier.get() == ts + depth
            assert supplier.get() == ts + depth - 1
            Completable.complete()
        }
        0 * _

        and:
        awaitCompleted(testSubscriber)
    }

    def "should scan latest articles and remove missing"() {
        given:
        def testSubscriber = TestSubscriber.create()
        def depth = 10
        def ts = 234234L

        when:
        target.apply(new ScanEvent(website, depth)).subscribe(testSubscriber)

        then:
        1 * websiteRepository.getLatestArticles(website, depth) >> just("b", "a")
        1 * linkScrapper.links(website) >> just(new Link(website, "c"), new Link(website, "a"))
        1 * articleRepository.get("c") >> Single.error(new RuntimeException())
        1 * articleRepository.get("a") >> Single.just(new Article("a", ""))
        1 * articleRepository.remove("b") >> Completable.complete()
        1 * currentMillis.get() >> ts
        1 * articleCollector.collect(new Link(website, "c"), _) >> Completable.complete()
        1 * articleCollector.collect(new Link(website, "a"), _) >> Completable.complete()
        0 * _

        and:
        awaitCompleted(testSubscriber)
    }
}