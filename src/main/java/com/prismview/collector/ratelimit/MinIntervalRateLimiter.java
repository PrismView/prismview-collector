package com.prismview.collector.ratelimit;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * 호출 사이 간격을 최소 minInterval로 유지한다.
 * OKX 호출 제한은 IP 기준이므로, 이 제한기는 앱 전체에서 하나만 공유해야 한다.
 */
public class MinIntervalRateLimiter {

    /** 잠자기를 테스트에서 바꿔 끼우기 위한 인터페이스 */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long nanos) throws InterruptedException;
    }

    private final long minIntervalNanos;
    private final LongSupplier nanoClock;
    private final Sleeper sleeper;

    private boolean acquiredBefore = false;
    private long lastAcquireNanos;

    /** 실제 실행용: 시스템 시계와 실제 잠자기 */
    public MinIntervalRateLimiter(Duration minInterval) {
        this(minInterval, System::nanoTime, TimeUnit.NANOSECONDS::sleep);
    }

    /** 테스트용: 가짜 시계와 가짜 잠자기 */
    public MinIntervalRateLimiter(Duration minInterval, LongSupplier nanoClock, Sleeper sleeper) {
        if (minInterval.isNegative()) {
            throw new IllegalArgumentException("minInterval은 음수일 수 없습니다: " + minInterval);
        }
        this.minIntervalNanos = minInterval.toNanos();
        this.nanoClock = nanoClock;
        this.sleeper = sleeper;
    }

    /** 직전 호출로부터 minInterval이 지나지 않았으면 남은 시간만큼 기다린다. */
    public synchronized void acquire() {
        long now = nanoClock.getAsLong();
        if (acquiredBefore) {
            long waitNanos = lastAcquireNanos + minIntervalNanos - now;
            if (waitNanos > 0) {
                try {
                    sleeper.sleep(waitNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("속도 제한 대기 중 중단됨", e);
                }
                now = nanoClock.getAsLong();
            }
        }
        lastAcquireNanos = now;
        acquiredBefore = true;
    }
}