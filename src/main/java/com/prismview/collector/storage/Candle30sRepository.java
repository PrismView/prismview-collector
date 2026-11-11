package com.prismview.collector.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * candle_30s 테이블 저장소.
 * candle_1s를 30초 버킷으로 집계해 upsert한다 (ADR-0006).
 */
@Repository
public class Candle30sRepository {

    private static final long BUCKET_MS = 30_000L;

    private static final String AGGREGATE_SQL = """
            INSERT INTO candle_30s (inst_id, ts, open, high, low, close, vol, vol_ccy, vol_quote, src_cnt)
            SELECT inst_id,
                   ts / 30000 * 30000                    AS bucket,
                   (array_agg(open  ORDER BY ts))[1],
                   max(high),
                   min(low),
                   (array_agg(close ORDER BY ts DESC))[1],
                   sum(vol),
                   sum(vol_ccy),
                   sum(vol_quote),
                   count(*)
            FROM candle_1s
            WHERE inst_id = ?
              AND ts >= ?
              AND ts <  ?
            GROUP BY inst_id, bucket
            ON CONFLICT (inst_id, ts) DO UPDATE SET
                open      = EXCLUDED.open,
                high      = EXCLUDED.high,
                low       = EXCLUDED.low,
                close     = EXCLUDED.close,
                vol       = EXCLUDED.vol,
                vol_ccy   = EXCLUDED.vol_ccy,
                vol_quote = EXCLUDED.vol_quote,
                src_cnt   = EXCLUDED.src_cnt
            """;

    private final JdbcTemplate jdbcTemplate;

    public Candle30sRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * [fromTs, toTs) 범위의 1초봉을 30초봉으로 집계해 upsert한다.
     * 범위는 30초 경계여야 한다. 반환값 = 처리된(새로 넣거나 갱신한) 30초봉 수.
     */
    public int aggregate(String instId, long fromTs, long toTs) {
        if (fromTs % BUCKET_MS != 0 || toTs % BUCKET_MS != 0) {
            throw new IllegalArgumentException(
                    "범위는 30초 경계여야 합니다: from=" + fromTs + " to=" + toTs);
        }
        if (fromTs >= toTs) {
            throw new IllegalArgumentException(
                    "from은 to보다 작아야 합니다: from=" + fromTs + " to=" + toTs);
        }
        return jdbcTemplate.update(AGGREGATE_SQL, instId, fromTs, toTs);
    }
}