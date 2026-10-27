package com.prismview.collector.storage;

import com.prismview.collector.domain.Candle;
import com.prismview.collector.domain.Timeframe;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCandleRepository implements CandleRepository {

    static final String UPSERT_SQL = """
            INSERT INTO candles (
                inst_id, timeframe, ts, open, high, low, close, vol, vol_ccy, vol_quote, confirmed
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (inst_id, timeframe, ts) DO UPDATE SET
                open = EXCLUDED.open,
                high = EXCLUDED.high,
                low = EXCLUDED.low,
                close = EXCLUDED.close,
                vol = EXCLUDED.vol,
                vol_ccy = EXCLUDED.vol_ccy,
                vol_quote = EXCLUDED.vol_quote,
                confirmed = EXCLUDED.confirmed
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCandleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsertAll(List<Candle> candles) {
        if (candles.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(UPSERT_SQL, candles, 1_000, (ps, candle) -> {
            ps.setString(1, candle.instId());
            ps.setString(2, candle.timeframe().code());
            ps.setLong(3, candle.ts());
            ps.setBigDecimal(4, candle.open());
            ps.setBigDecimal(5, candle.high());
            ps.setBigDecimal(6, candle.low());
            ps.setBigDecimal(7, candle.close());
            ps.setBigDecimal(8, candle.vol());
            ps.setBigDecimal(9, candle.volCcy());
            ps.setBigDecimal(10, candle.volQuote());
            ps.setBoolean(11, candle.confirmed());
        });
    }

    @Override
    public Optional<Long> findLatestTs(String instId, Timeframe timeframe) {
        List<Long> rows = jdbcTemplate.query(
                "SELECT MAX(ts) AS ts FROM candles WHERE inst_id = ? AND timeframe = ?",
                (rs, rowNum) -> {
                    long ts = rs.getLong("ts");
                    return rs.wasNull() ? null : ts;
                },
                instId,
                timeframe.code()
        );
        if (rows.isEmpty() || rows.getFirst() == null) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }
}
