package com.prismview.collector.retry;

import com.prismview.collector.okx.OkxApiException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * 일시적인 오류(네트워크 끊김, 5xx, 429, OKX 속도 제한 코드)만 지수 백오프로 다시 시도한다.
 * 재시도해도 소용없는 오류(잘못된 요청 등)는 바로 던진다.
 */
public class Retrier {

    private static final Logger log = LoggerFactory.getLogger(Retrier.class);

    /** 대기 시간이 끝없이 늘어나지 않도록 하는 상한 */
    private static final Duration MAX_DELAY = Duration.ofSeconds(30);

    /** OKX 업무 오류 중 재시도할 코드: 50011 요청 과다(HTTP 200으로 오는 경우). 429로 오는 경우는 HTTP 오류로 처리 */
    private static final Set<String> RETRYABLE_OKX_CODES = Set.of("50011");

    /** 잠자기를 테스트에서 바꿔 끼우기 위한 인터페이스 */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long nanos) throws InterruptedException;
    }

    private final int maxAttempts;
    private final Duration initialDelay;
    private final Sleeper sleeper;

    /** 실제 실행용: 실제 잠자기 */
    public Retrier(int maxAttempts, Duration initialDelay) {
        this(maxAttempts, initialDelay, TimeUnit.NANOSECONDS::sleep);
    }

    /** 테스트용: 가짜 잠자기 */
    public Retrier(int maxAttempts, Duration initialDelay, Sleeper sleeper) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts는 1 이상이어야 합니다: " + maxAttempts);
        }
        if (initialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay는 음수일 수 없습니다: " + initialDelay);
        }
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.sleeper = sleeper;
    }

    /** action을 실행하고, 재시도 가능한 오류면 최대 maxAttempts번까지 다시 시도한다. */
    public <T> T call(String label, Supplier<T> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                if (!isRetryable(e) || attempt >= maxAttempts) {
                    throw e;
                }
                Duration delay = delayBefore(attempt);
                log.warn("[{}] 실패 {}/{}회, {}ms 후 재시도: {}",
                        label, attempt, maxAttempts, delay.toMillis(), e.toString());
                sleep(delay);
            }
        }
    }

    /** attempt번째 실패 뒤의 대기 시간: initialDelay × 2^(attempt-1), 상한 MAX_DELAY */
    Duration delayBefore(int attempt) {
        Duration delay = initialDelay;
        for (int i = 1; i < attempt; i++) {
            delay = delay.multipliedBy(2);
            if (delay.compareTo(MAX_DELAY) >= 0) {
                return MAX_DELAY;
            }
        }
        return delay.compareTo(MAX_DELAY) > 0 ? MAX_DELAY : delay;
    }

    static boolean isRetryable(RuntimeException e) {
        if (e instanceof OkxApiException okx) {
            return RETRYABLE_OKX_CODES.contains(okx.code());
        }
        if (e instanceof HttpClientErrorException http) {
            return http.getStatusCode().value() == 429; // 4xx 중 요청 과다만 재시도
        }
        return e instanceof RestClientException; // 연결 실패, 타임아웃, 5xx 등
    }

    private void sleep(Duration delay) {
        try {
            sleeper.sleep(delay.toNanos());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("재시도 대기 중 중단됨", ie);
        }
    }
}
