package ua.devchallenge.persistence

import org.springframework.beans.factory.annotation.Autowired
import rx.observers.TestSubscriber
import ua.devchallenge.FunctionalTestSpecification
import ua.devchallenge.domain.Article
import ua.devchallenge.domain.Website

import static ua.devchallenge.fixture.RxFixture.awaitCompleted

class WebsiteRepositorySpec extends FunctionalTestSpecification {

    @Autowired
    WebsiteRepository websiteRepository

    def websiteUrl = UUID.randomUUID().toString()

    def website = Website.builder()
            .url(websiteUrl)
            .prevButtonXPath("//li[contains(@class, 'prev')]/a")
            .nextButtonXPath("//li[contains(@class, 'next')]/a")
            .linkXpath("//h2[contains(@class, 'post-title')]/a")
            .maxScanDepth(100)
            .latestPercent(0.1)
            .build();

    def "should save & get website"() {
        given:
        def saveSubscriber = TestSubscriber.create()
        websiteRepository.save(website).subscribe(saveSubscriber)
        awaitCompleted(saveSubscriber)

        expect:
        websiteRepository.get(websiteUrl).toBlocking().value() == website
    }

    def "should add website to set when save is called"() {
        given:
        def saveSubscriber = TestSubscriber.create()
        websiteRepository.save(website).subscribe(saveSubscriber)
        awaitCompleted(saveSubscriber)

        expect:
        websiteRepository.all().toList().toBlocking().single() == [websiteUrl]
    }

    def "should add articles to website with ts and get articles"() {
        given:
        def saveSubscriber = TestSubscriber.create()
        websiteRepository.save(website).subscribe(saveSubscriber)
        awaitCompleted(saveSubscriber)

        and:
        def firstArticleUrl = "${websiteUrl}/1"
        def secondArticleUrl = "${websiteUrl}/2"
        def firstArticle = new Article(firstArticleUrl, "1\n2")
        def secondArticle = new Article(secondArticleUrl, "2")
        def firstArticleSubscriber = TestSubscriber.create()
        def secondArticleSubscriber = TestSubscriber.create()

        websiteRepository.updateArticleSet(website, firstArticle, 0).subscribe(firstArticleSubscriber)
        websiteRepository.updateArticleSet(website, secondArticle, 1).subscribe(secondArticleSubscriber)

        awaitCompleted(firstArticleSubscriber)
        awaitCompleted(secondArticleSubscriber)

        expect:
        websiteRepository.getArticles(websiteUrl).toList().toBlocking().single() == [firstArticleUrl, secondArticleUrl]
    }

    def "should add articles to website with ts and get latest"() {
        given:
        def saveSubscriber = TestSubscriber.create()
        websiteRepository.save(website).subscribe(saveSubscriber)
        awaitCompleted(saveSubscriber)

        and:
        def firstArticleUrl = "${websiteUrl}/1"
        def secondArticleUrl = "${websiteUrl}/2"
        def firstArticle = new Article(firstArticleUrl, "1\n2")
        def secondArticle = new Article(secondArticleUrl, "2")
        def firstArticleSubscriber = TestSubscriber.create()
        def secondArticleSubscriber = TestSubscriber.create()

        websiteRepository.updateArticleSet(website, firstArticle, 0).subscribe(firstArticleSubscriber)
        websiteRepository.updateArticleSet(website, secondArticle, 1).subscribe(secondArticleSubscriber)

        awaitCompleted(firstArticleSubscriber)
        awaitCompleted(secondArticleSubscriber)

        expect:
        websiteRepository.getLatestArticles(website, 100L).toList().toBlocking().single() == [secondArticleUrl, firstArticleUrl]
    }
}
