package com.prismview.collector.aggregate;

import com.prismview.collector.domain.Candle;
import com.prismview.collector.domain.Timeframe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 확정된 1초봉 30개를 한 개의 30초봉으로 접는다.
 * 개수가 모자란 버킷은 만들지 않는다. 누락 검사는 integrity 패키지에서 따로 한다.
 */
public final class CandleAggregator {

    private static final int SECONDS_PER_THIRTY = 30;

    private CandleAggregator() {
    }

    public static List<Candle> toThirtySecond(List<Candle> oneSecondCandles) {
        Map<Long, List<Candle>> byBucket = oneSecondCandles.stream()
                .filter(Candle::confirmed)
                .filter(candle -> candle.timeframe() == Timeframe.S1)
                .sorted(Comparator.comparingLong(Candle::ts))
                .collect(Collectors.groupingBy(candle -> candle.bucketTs(Timeframe.S30.durationMs())));

        List<Candle> result = new ArrayList<>();
        byBucket.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<Candle> bucket = entry.getValue();
                    if (bucket.size() != SECONDS_PER_THIRTY) {
                        return;
                    }
                    result.add(fold(bucket.getFirst().instId(), entry.getKey(), bucket));
                });
        return result;
    }

    private static Candle fold(String instId, long bucketTs, List<Candle> bucket) {
        Candle first = bucket.getFirst();
        Candle last = bucket.getLast();
        BigDecimal high = bucket.stream().map(Candle::high).max(BigDecimal::compareTo).orElseThrow();
        BigDecimal low = bucket.stream().map(Candle::low).min(BigDecimal::compareTo).orElseThrow();
        BigDecimal vol = sum(bucket, Candle::vol);
        BigDecimal volCcy = sum(bucket, Candle::volCcy);
        BigDecimal volQuote = sum(bucket, Candle::volQuote);
        return new Candle(
                instId,
                Timeframe.S30,
                bucketTs,
                first.open(),
                high,
                low,
                last.close(),
                vol,
                volCcy,
                volQuote,
                true
        );
    }

    private static BigDecimal sum(List<Candle> bucket, java.util.function.Function<Candle, BigDecimal> getter) {
        return bucket.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
