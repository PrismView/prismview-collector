package com.prismview.collector.storage;

import com.prismview.collector.domain.Candle;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * candle_1s 테이블 저장소.
 * 이 테이블에는 확정 봉(confirm=true)만 저장한다 (V2 주석, ADR-0005).
 */
@Repository
public class Candle1sRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO candle_1s (inst_id, ts, open, high, low, close, vol, vol_ccy, vol_quote)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (inst_id, ts) DO UPDATE SET
                open      = EXCLUDED.open,
                high      = EXCLUDED.high,
                low       = EXCLUDED.low,
                close     = EXCLUDED.close,
                vol       = EXCLUDED.vol,
                vol_ccy   = EXCLUDED.vol_ccy,
                vol_quote = EXCLUDED.vol_quote
            """;

    private static final String MIN_TS_SQL =
            "SELECT min(ts) FROM candle_1s WHERE inst_id = ?";

    private final JdbcTemplate jdbcTemplate;

    public Candle1sRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 한 페이지를 한 번의 배치로 upsert한다. 확정되지 않은 봉이 섞여 있으면 하나도 저장하지 않고 예외. */
    public void upsertAll(List<Candle> candles) {
        if (candles.isEmpty()) {
            return;
        }
        for (Candle c : candles) {
            if (!c.confirm()) {
                throw new IllegalArgumentException(
                        "확정되지 않은 봉은 저장할 수 없습니다: " + c.instId() + " " + c.ts());
            }
        }

        jdbcTemplate.batchUpdate(UPSERT_SQL, candles, candles.size(), (ps, c) -> {
            ps.setString(1, c.instId());
            ps.setLong(2, c.ts());
            ps.setBigDecimal(3, c.open());
            ps.setBigDecimal(4, c.high());
            ps.setBigDecimal(5, c.low());
            ps.setBigDecimal(6, c.close());
            ps.setBigDecimal(7, c.vol());
            ps.setBigDecimal(8, c.volCcy());
            ps.setBigDecimal(9, c.volQuote());
        });
    }

    /** 종목의 가장 오래된 ts. 데이터가 없으면 빈 값 → 백필은 최신부터 시작. */
    public Optional<Long> findMinTs(String instId) {
        Long minTs = jdbcTemplate.queryForObject(MIN_TS_SQL, Long.class, instId);
        return Optional.ofNullable(minTs);
    }
}