package com.prismview.collector.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * OKX 캔들 한 개. ts는 봉 시작 시각(UTC epoch ms).
 * SWAP의 vol은 계약 수, volCcy는 기초자산 수량, volQuote는 USDT 금액.
 */
public record Candle(
        String instId,
        Timeframe timeframe,
        long ts,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal vol,
        BigDecimal volCcy,
        BigDecimal volQuote,
        boolean confirmed
) {

    /**
     * OKX 응답 한 줄: ["ts","o","h","l","c","vol","volCcy","volCcyQuote","confirm"]
     */
    public static Candle fromOkx(String instId, Timeframe timeframe, List<String> row) {
        return new Candle(
                instId,
                timeframe,
                Long.parseLong(row.get(0)),
                new BigDecimal(row.get(1)),
                new BigDecimal(row.get(2)),
                new BigDecimal(row.get(3)),
                new BigDecimal(row.get(4)),
                new BigDecimal(row.get(5)),
                new BigDecimal(row.get(6)),
                new BigDecimal(row.get(7)),
                "1".equals(row.get(8))
        );
    }

    public long bucketTs(long durationMs) {
        return ts / durationMs * durationMs;
    }
}
