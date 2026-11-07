package com.prismview.collector.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinIntervalRateLimiterTest {

    private static final long MS = 1_000_000L; // 1ms = 1,000,000ns

    private long now;                           // 가짜 시계의 현재 시각(ns)
    private List<Long> sleeps;                  // 실제로 잠든 시간 기록
    private MinIntervalRateLimiter limiter;

    @BeforeEach
    void setUp() {
        now = 0;
        sleeps = new ArrayList<>();
        limiter = new MinIntervalRateLimiter(
                Duration.ofMillis(125),
                () -> now,
                nanos -> {                      // 잠자는 대신 기록하고 시계만 앞으로
                    sleeps.add(nanos);
                    now += nanos;
                });
    }

    @Test
    void 첫_호출은_기다리지_않는다() {
        limiter.acquire();

        assertThat(sleeps).isEmpty();
    }

    @Test
    void 바로_이어서_호출하면_간격만큼_기다린다() {
        limiter.acquire();
        limiter.acquire();

        assertThat(sleeps).containsExactly(125 * MS);
    }

    @Test
    void 간격보다_오래_지났으면_기다리지_않는다() {
        limiter.acquire();
        now += 200 * MS;
        limiter.acquire();

        assertThat(sleeps).isEmpty();
    }

    @Test
    void 일부만_지났으면_남은_시간만큼만_기다린다() {
        limiter.acquire();
        now += 50 * MS;
        limiter.acquire();

        assertThat(sleeps).containsExactly(75 * MS);
    }

    @Test
    void 연속_호출의_간격은_항상_최소_간격_이상이다() {
        List<Long> acquiredAt = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            limiter.acquire();
            acquiredAt.add(now);
        }

        for (int i = 1; i < acquiredAt.size(); i++) {
            assertThat(acquiredAt.get(i) - acquiredAt.get(i - 1)).isGreaterThanOrEqualTo(125 * MS);
        }
    }
}