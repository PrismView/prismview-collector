package com.prismview.collector.backfill;

import com.prismview.collector.domain.Candle;
import com.prismview.collector.ratelimit.MinIntervalRateLimiter;
import com.prismview.collector.retry.Retrier;
import com.prismview.collector.storage.Candle1sRepository;
import java.time.Duration; // 추가
import java.time.Instant; // 추가
import java.util.List;
import org.slf4j.Logger; // 추가
import org.slf4j.LoggerFactory; // 추가
import org.springframework.stereotype.Service;

/**
- 한 종목의 1초봉을 OKX 제공 범위 끝까지 과거 방향으로 받아 저장한다.
- 중단 후 다시 실행하면 DB의 가장 오래된 ts부터 이어서 받는다.
 */
@Service
public class BackfillService {

    private static final Logger log = LoggerFactory.getLogger(BackfillService.class); // 추가
    private static final int LOG_EVERY_PAGES = 100; // 100페이지 = 1만 행 ≈ 12.5초마다 추가

    private final CandlePageSource source;
    private final Candle1sRepository repository;
    private final MinIntervalRateLimiter rateLimiter;
    private final Retrier retrier;

    public BackfillService(CandlePageSource source, Candle1sRepository repository,
                           MinIntervalRateLimiter rateLimiter, Retrier retrier) {
        this.source = source;
        this.repository = repository;
        this.rateLimiter = rateLimiter;
        this.retrier = retrier;
    }

    public Result backfill(String instId) {
        // 재개 지점: DB에 데이터가 있으면 가장 오래된 ts부터, 없으면 null(최신부터)
        Long cursor = repository.findMinTs(instId).orElse(null);
        long pages = 0;
        long saved = 0;
        long startNanos = System.nanoTime(); // 추가

        log.info("[{}] 백필 시작, 시작 커서={}", instId, toUtc(cursor)); // 추가

        while (true) {
            // 재시도 때마다 속도 제한을 다시 거쳐야 OKX 호출 제한을 넘지 않는다
            final Long after = cursor;
            List<Candle> page = retrier.call(instId, () -> {
                rateLimiter.acquire();
                return source.fetchOnePage(instId, after);
            });
            if (page.isEmpty()) {
                break; // OKX 제공 범위 끝
            }

            long oldest = page.stream().mapToLong(Candle::ts).min().orElseThrow();
            // 커서가 줄어들지 않으면 같은 구간을 무한히 다시 받게 된다 → 저장 전에 멈춤
            if (cursor != null && oldest >= cursor) {
                throw new IllegalStateException(
                        "커서가 줄어들지 않음: instId=" + instId + ", cursor=" + cursor + ", oldest=" + oldest);
            }

            List<Candle> confirmed = page.stream().filter(Candle::confirm).toList();
            repository.upsertAll(confirmed);

            cursor = oldest;
            pages++;
            saved += confirmed.size();

            if (pages % LOG_EVERY_PAGES == 0) { // 추가
                log.info("[{}] 진행: 커서={}, 페이지={}, 저장={}, 경과={}",
                        instId, toUtc(cursor), pages, saved,
                        Duration.ofNanos(System.nanoTime() - startNanos).toSeconds() + "s");
            }
        } // while 루프 종료 (누락되었던 괄호 추가)

        log.info("[{}] 백필 완료: 총 페이지={}, 저장={}, 최종 커서={}",
                instId, pages, saved, toUtc(cursor));

        return new Result(instId, pages, saved, cursor);
    }

    /** 로그용: epoch ms → UTC 시각 (null이면 "없음(최신부터)") */
    private static String toUtc(Long epochMs) { // 추가
        return epochMs == null ? "없음(최신부터)" : Instant.ofEpochMilli(epochMs).toString();
    }

    /** 백필 결과: 받은 페이지 수, 저장한 봉 수, 마지막 커서(= 받은 범위의 가장 오래된 ts) */
    public record Result(String instId, long pages, long saved, Long oldestTs) {
    }
}