package au.com.example.webchat.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class WebChatServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebChatServerApplication.class, args);
        log.info("=================================================");
        log.info("WebChat Server started successfully!");
        log.info("=================================================");
    }
}
