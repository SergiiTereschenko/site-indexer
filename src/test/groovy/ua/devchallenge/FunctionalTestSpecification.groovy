package ua.devchallenge

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import redis.embedded.RedisServer
import spock.lang.Specification

@ContextConfiguration
@SpringBootTest(classes = Test)
public class FunctionalTestSpecification extends Specification {

    private RedisServer redisServer;

    void setup() {
        redisServer = new RedisServer(6379)
        redisServer.start()
    }

    void cleanup() {
        redisServer.stop()
    }

    @Configuration
    @Import([IndexerApplication])
    static class Test {
    }

}
