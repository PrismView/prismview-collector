package com.prismview.collector.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prismview.collector.okx.OkxApiException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class RetrierTest {

    /** 실제로 자지 않고 대기 시간(ms)만 기록 */
    private final List<Long> sleptMillis = new ArrayList<>();

    private Retrier retrier(int maxAttempts, Duration initialDelay) {
        return new Retrier(maxAttempts, initialDelay, nanos -> sleptMillis.add(nanos / 1_000_000));
    }

    @Test
    void 첫_시도에_성공하면_기다리지_않는다() {
        String result = retrier(3, Duration.ofSeconds(1)).call("t", () -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(sleptMillis).isEmpty();
    }

    @Test
    void 일시_오류는_지수_백오프로_재시도해_성공하면_결과를_돌려준다() {
        AtomicInteger calls = new AtomicInteger();

        String result = retrier(5, Duration.ofSeconds(1)).call("t", () -> {
            if (calls.incrementAndGet() < 3) {
                throw new ResourceAccessException("연결 끊김");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(3);
        assertThat(sleptMillis).containsExactly(1000L, 2000L);
    }

    @Test
    void 계속_실패하면_최대_시도_횟수만큼만_하고_마지막_예외를_던진다() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> retrier(3, Duration.ofSeconds(1)).call("t", () -> {
            calls.incrementAndGet();
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        })).isInstanceOf(HttpServerErrorException.class);

        assertThat(calls).hasValue(3);
        assertThat(sleptMillis).containsExactly(1000L, 2000L); // 마지막 실패 뒤에는 기다리지 않음
    }

    @Test
    void 속도_제한_코드의_OKX_오류는_재시도한다() {
        AtomicInteger calls = new AtomicInteger();

        String result = retrier(3, Duration.ofMillis(10)).call("t", () -> {
            if (calls.incrementAndGet() == 1) {
                throw new OkxApiException("50011", "Too Many Requests");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(2);
    }

    @Test
    void 재시도해도_소용없는_오류는_바로_던진다() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> retrier(5, Duration.ofSeconds(1)).call("t", () -> {
            calls.incrementAndGet();
            throw new OkxApiException("51001", "Instrument ID does not exist");
        })).isInstanceOf(OkxApiException.class);

        assertThatThrownBy(() -> retrier(5, Duration.ofSeconds(1)).call("t", () -> {
            calls.incrementAndGet();
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        })).isInstanceOf(HttpClientErrorException.class);

        assertThatThrownBy(() -> retrier(5, Duration.ofSeconds(1)).call("t", () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("버그");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(calls).hasValue(3); // 각 1번씩만 시도
        assertThat(sleptMillis).isEmpty();
    }

    @Test
    void 요청_과다_429는_재시도한다() {
        AtomicInteger calls = new AtomicInteger();

        String result = retrier(3, Duration.ofMillis(10)).call("t", () -> {
            if (calls.incrementAndGet() == 1) {
                throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(2);
    }

    @Test
    void 대기_시간은_30초를_넘지_않는다() {
        Retrier r = retrier(10, Duration.ofSeconds(10));

        assertThat(r.delayBefore(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(r.delayBefore(2)).isEqualTo(Duration.ofSeconds(20));
        assertThat(r.delayBefore(3)).isEqualTo(Duration.ofSeconds(30));
        assertThat(r.delayBefore(9)).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void 최대_시도_횟수가_1_미만이면_만들_수_없다() {
        assertThatThrownBy(() -> new Retrier(0, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
