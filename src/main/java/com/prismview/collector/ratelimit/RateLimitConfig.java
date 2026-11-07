package com.prismview.collector.ratelimit;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    /** OKX 호출 제한은 IP 기준 → 앱 전체에서 하나의 제한기를 공유 */
    @Bean
    public MinIntervalRateLimiter okxRateLimiter(
            @Value("${okx.rate-limit.min-interval}") Duration minInterval) {
        return new MinIntervalRateLimiter(minInterval);
    }
}