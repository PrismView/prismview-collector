package com.prismview.collector.aggregate;

import com.prismview.collector.domain.Candle;
import com.prismview.collector.domain.Timeframe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CandleAggregatorTest {

    @Test
    void foldsThirtyConfirmedSecondsIntoOneBar() {
        long startTs = 1_699_999_980_000L;
        List<Candle> seconds = secondCandles(startTs, 30);
        List<Candle> thirty = CandleAggregator.toThirtySecond(seconds);

        assertThat(thirty).hasSize(1);
        Candle bar = thirty.getFirst();
        assertThat(bar.timeframe()).isEqualTo(Timeframe.S30);
        assertThat(bar.ts()).isEqualTo(startTs);
        assertThat(bar.open()).isEqualByComparingTo("100");
        assertThat(bar.close()).isEqualByComparingTo("129");
        assertThat(bar.high()).isEqualByComparingTo("129");
        assertThat(bar.low()).isEqualByComparingTo("100");
        assertThat(bar.vol()).isEqualByComparingTo("30");
    }

    @Test
    void skipsIncompleteBucket() {
        List<Candle> seconds = secondCandles(1_699_999_980_000L, 29);

        assertThat(CandleAggregator.toThirtySecond(seconds)).isEmpty();
    }

    private static List<Candle> secondCandles(long startTs, int count) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BigDecimal price = BigDecimal.valueOf(100 + i);
            candles.add(new Candle(
                    "BTC-USDT-SWAP",
                    Timeframe.S1,
                    startTs + i * 1_000L,
                    price,
                    price,
                    price,
                    price,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.TEN,
                    true
            ));
        }
        return candles;
    }
}
