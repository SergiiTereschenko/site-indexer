package ua.devchallenge.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rx.Observable;
import rx.Single;
import ua.devchallenge.domain.Website;
import ua.devchallenge.domain.dto.ScanEvent;
import ua.devchallenge.persistence.WebsiteRepository;
import ua.devchallenge.service.WebsiteScanner;

import static rx.Single.just;

@RestController
@RequestMapping("/website")
@RequiredArgsConstructor
public class WebsiteController {

    private final WebsiteRepository websiteRepository;
    private final WebsiteScanner websiteScanner;

    @PostMapping
    public Single<Website> save(@RequestBody Website website) {
        return websiteRepository.save(website)
            .andThen(just(website));
    }

    @GetMapping
    public Single<Website> get(@RequestParam String url) {
        return websiteRepository.get(url);
    }

    @GetMapping("/articles")
    public Observable<String> articles(@RequestParam String url) {
        return websiteRepository.getArticles(url);
    }

    @PostMapping("/scan/latest")
    public Single<ScanEvent> scanLatest(@RequestParam String url) {
        return websiteRepository.get(url)
            .flatMap(website -> {
                ScanEvent event = new ScanEvent(website, (int) (website.getMaxScanDepth() * website.getLatestPercent()));
                websiteScanner.apply(event).subscribe();
                return just(event);
            });
    }

    @PostMapping("/scan/all")
    public Single<ScanEvent> scanAll(@RequestParam String url) {
        return websiteRepository.get(url)
            .flatMap(website -> {
                ScanEvent event = new ScanEvent(website, website.getMaxScanDepth());
                websiteScanner.apply(event).subscribe();
                return just(event);
            });
    }

}
