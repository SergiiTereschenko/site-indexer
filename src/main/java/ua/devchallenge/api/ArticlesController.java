package ua.devchallenge.api;

import difflib.Patch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rx.Single;
import ua.devchallenge.domain.Article;
import ua.devchallenge.persistence.ArticleRepository;

@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
public class ArticlesController {

    private final ArticleRepository articleRepository;

    @GetMapping
    public Single<String> article(String url) {
        return articleRepository.get(url)
            .map(Article::getBody);
    }

    @GetMapping("/revision")
    public Single<String> revision(String url, int v) {
        return articleRepository.getRevision(url, v)
            .map(Article::getBody);
    }

    @GetMapping("/changelog")
    public Single<Patch<String>> changelog(String url, int v1, int v2) {
        return articleRepository.getDiff(url, v1, v2);
    }

}
