package com.prismview.collector.backfill;

import com.prismview.collector.domain.Candle;
import com.prismview.collector.storage.Candle1sRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 한 종목의 1초봉을 OKX 제공 범위 끝까지 과거 방향으로 받아 저장한다.
 * 중단 후 다시 실행하면 DB의 가장 오래된 ts부터 이어서 받는다.
 */
@Service
public class BackfillService {

    private final CandlePageSource source;
    private final Candle1sRepository repository;

    public BackfillService(CandlePageSource source, Candle1sRepository repository) {
        this.source = source;
        this.repository = repository;
    }

    public Result backfill(String instId) {
        // 재개 지점: DB에 데이터가 있으면 가장 오래된 ts부터, 없으면 null(최신부터)
        Long cursor = repository.findMinTs(instId).orElse(null);
        long pages = 0;
        long saved = 0;

        while (true) {
            List<Candle> page = source.fetchOnePage(instId, cursor);
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
        }

        return new Result(instId, pages, saved, cursor);
    }

    /** 백필 결과: 받은 페이지 수, 저장한 봉 수, 마지막 커서(= 받은 범위의 가장 오래된 ts) */
    public record Result(String instId, long pages, long saved, Long oldestTs) {
    }
}