package ua.devchallenge.scheduling

import rx.Observable
import rx.Single
import spock.lang.Specification
import ua.devchallenge.domain.Website
import ua.devchallenge.domain.dto.ScanEvent
import ua.devchallenge.persistence.WebsiteRepository
import ua.devchallenge.service.WebsiteScanner

import static rx.Completable.complete

class ScheduledWebsiteScannerSpec extends Specification {

    WebsiteRepository websiteRepository = Mock()
    WebsiteScanner scanner = Mock()

    def target = new ScheduledScanner(websiteRepository, scanner)

    def "should scan all sites"() {
        given:
        def urls = ["a", "b"]
        def firstWebsite = Website.builder()
                .maxScanDepth(100)
                .latestPercent(0.1)
                .build()
        def secondWebsite = Website.builder()
                .maxScanDepth(200)
                .latestPercent(0.5)
                .build()
        def firstEvent = new ScanEvent(firstWebsite, 10);
        def secondEvent = new ScanEvent(secondWebsite, 100);

        when:
        target.scan()

        then:
        1 * websiteRepository.all() >> Observable.from(urls as List)
        1 * websiteRepository.get("a") >> Single.just(firstWebsite)
        1 * websiteRepository.get("b") >> Single.just(secondWebsite)
        1 * scanner.apply(firstEvent) >> complete()
        1 * scanner.apply(secondEvent) >> complete()
        0 * _
    }
}
