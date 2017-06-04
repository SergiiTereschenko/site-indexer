package ua.devchallenge.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.devchallenge.domain.dto.ScanEvent;
import ua.devchallenge.persistence.WebsiteRepository;
import ua.devchallenge.service.WebsiteScanner;

@Service
@ConditionalOnProperty(value = "scheduled.scan.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ScheduledScanner {

    private final WebsiteRepository websiteRepository;
    private final WebsiteScanner websiteScanner;

    @Scheduled(fixedDelayString = "${scheduled.scan.delay}")
    public void scan() {
        websiteRepository.all()
            .flatMap(url -> websiteRepository.get(url).toObservable())
            .flatMap(website -> {
                ScanEvent event = new ScanEvent(website, (int) (website.getMaxScanDepth() * website.getLatestPercent()));
                return websiteScanner.apply(event).toObservable();
            })
            .toList()
            .toBlocking()
            .single();
    }
}
