package dev.videobridge;

import dev.videobridge.config.BridgeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(BridgeProperties.class)
@EnableScheduling
public class VideoBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoBridgeApplication.class, args);
    }
}
