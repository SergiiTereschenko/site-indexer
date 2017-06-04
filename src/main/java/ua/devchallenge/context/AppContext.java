package ua.devchallenge.context;

import java.util.function.Supplier;

import com.gargoylesoftware.htmlunit.WebClient;
import io.vertx.redis.RedisOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.redis.RedisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppContext {

    @Bean
    WebClient webClient() {
        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        return client;
    }

    @Bean
    Vertx vertx() {
        return Vertx.vertx();
    }

    @Bean
    io.vertx.rxjava.ext.web.client.WebClient rxWebClient() {
        return io.vertx.rxjava.ext.web.client.WebClient.create(vertx());
    }

    @Bean
    RedisClient redisClient(@Value("${spring.redis.host}") String redisHost,
                            @Value("${spring.redis.port}") int redisPort) {
        RedisOptions config = new RedisOptions()
            .setHost(redisHost)
            .setPort(redisPort);

        return RedisClient.create(vertx(), config);
    }

    @Bean
    Supplier<Long> currentMillis() {
        return System::currentTimeMillis;
    }


}
