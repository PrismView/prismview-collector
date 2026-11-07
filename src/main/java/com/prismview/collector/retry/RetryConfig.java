package com.prismview.collector.retry;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {

    @Bean
    public Retrier okxRetrier(
            @Value("${okx.retry.max-attempts}") int maxAttempts,
            @Value("${okx.retry.initial-delay}") Duration initialDelay) {
        return new Retrier(maxAttempts, initialDelay);
    }
}
