package dev.videobridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AppConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** 下载定时关流、永久流保活共用，关服务时一起停掉 */
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService bridgeScheduler() {
        return Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "video-bridge-scheduler");
            t.setDaemon(true);
            return t;
        });
    }
}
