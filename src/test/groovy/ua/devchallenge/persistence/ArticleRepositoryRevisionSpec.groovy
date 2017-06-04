package ua.devchallenge.persistence

import difflib.ChangeDelta
import difflib.Chunk
import difflib.DeleteDelta
import org.springframework.beans.factory.annotation.Autowired
import rx.observers.TestSubscriber
import ua.devchallenge.FunctionalTestSpecification
import ua.devchallenge.domain.Article

import static ua.devchallenge.fixture.RxFixture.awaitCompleted
import static ua.devchallenge.fixture.RxFixture.awaitValue

class ArticleRepositoryRevisionSpec extends FunctionalTestSpecification {

    @Autowired
    ArticleRepository articleRepository

    def articleUrl = UUID.randomUUID().toString()

    def "should save article"() {
        given:
        def article = new Article(articleUrl, "1\n2")
        def testSubscriber = TestSubscriber.create()

        when:
        articleRepository.save(article).subscribe(testSubscriber)

        then:
        awaitCompleted(testSubscriber)

        and:
        articleRepository.getRevision(articleUrl, 0).toBlocking().value() == article
    }

    def "should get article"() {
        given:
        def article = new Article(articleUrl, "1\n2")
        def testSubscriber = TestSubscriber.create()

        articleRepository.save(article).subscribe(testSubscriber)
        awaitCompleted(testSubscriber)

        when:
        def getSubscriber = TestSubscriber.create()
        articleRepository.get(articleUrl).subscribe(getSubscriber)

        then:
        awaitValue(getSubscriber, article)
    }

    def "should remove article"() {
        given:
        def article = new Article(articleUrl, "1\n2")
        def removedArticle = new Article(articleUrl, "")
        def testSubscriber = TestSubscriber.create()

        articleRepository.save(article).subscribe(testSubscriber)
        awaitCompleted(testSubscriber)

        when:
        def removalSubscriber = TestSubscriber.create()
        articleRepository.remove(articleUrl).subscribe(removalSubscriber)

        then:
        awaitCompleted(removalSubscriber)

        and:
        articleRepository.get(articleUrl).toBlocking().value() == removedArticle
        articleRepository.getRevision(articleUrl, 1).toBlocking().value() == removedArticle
    }

    def "should recover revision"() {
        given:
        def firstArticle = new Article(articleUrl, "1\n2")
        def secondArticle = new Article(articleUrl, "2")
        def thirdArticle = new Article(articleUrl, "3\n4\n5")
        def testSubscriber = TestSubscriber.create()

        articleRepository.save(firstArticle)
                .andThen(articleRepository.save(secondArticle))
                .andThen(articleRepository.save(thirdArticle))
                .subscribe(testSubscriber)
        awaitCompleted(testSubscriber)

        expect:
        articleRepository.getRevision(articleUrl, 0).toBlocking().value() == firstArticle
        articleRepository.getRevision(articleUrl, 1).toBlocking().value() == secondArticle
        articleRepository.getRevision(articleUrl, 2).toBlocking().value() == thirdArticle
    }

    def "should get diff"() {
        given:
        def firstArticle = new Article(articleUrl, "1\n2")
        def secondArticle = new Article(articleUrl, "2")
        def thirdArticle = new Article(articleUrl, "3\n4\n5")
        def testSubscriber = TestSubscriber.create()

        articleRepository.save(firstArticle)
                .andThen(articleRepository.save(secondArticle))
                .andThen(articleRepository.save(thirdArticle))
                .subscribe(testSubscriber)
        awaitCompleted(testSubscriber)

        expect:
        articleRepository.getDiff(articleUrl, 0, 0).toBlocking().value().getDeltas() == []
        articleRepository.getDiff(articleUrl, 0, 1).toBlocking().value().getDeltas() == [new DeleteDelta(new Chunk(0, ["1"]), new Chunk(0, []))]
        articleRepository.getDiff(articleUrl, 1, 1).toBlocking().value().getDeltas() == []
        articleRepository.getDiff(articleUrl, 1, 2).toBlocking().value().getDeltas() == [new ChangeDelta(new Chunk(0, ["2"]), new Chunk(0, ["3", "4", "5"]))]
        articleRepository.getDiff(articleUrl, 2, 2).toBlocking().value().getDeltas() == []
        articleRepository.getDiff(articleUrl, 0, 2).toBlocking().value().getDeltas() == [new ChangeDelta(new Chunk(0, ["1", "2"]), new Chunk(0, ["3", "4", "5"]))]

    }

}
