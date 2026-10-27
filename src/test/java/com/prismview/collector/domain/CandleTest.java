package com.prismview.collector.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CandleTest {

    @Test
    void fromOkxMapsRowAndConfirmedFlag() {
        Candle candle = Candle.fromOkx(
                "BTC-USDT-SWAP",
                Timeframe.S1,
                List.of("1700000000000", "65000.1", "65010", "64990", "65005", "1.5", "0.015", "975.0", "1")
        );

        assertThat(candle.ts()).isEqualTo(1_700_000_000_000L);
        assertThat(candle.open()).isEqualByComparingTo(new BigDecimal("65000.1"));
        assertThat(candle.confirmed()).isTrue();
        assertThat(candle.bucketTs(Timeframe.S30.durationMs())).isEqualTo(1_699_999_980_000L);
    }

    @Test
    void fromOkxTreatsConfirmZeroAsUnconfirmed() {
        Candle candle = Candle.fromOkx(
                "ETH-USDT-SWAP",
                Timeframe.S1,
                List.of("1", "1", "1", "1", "1", "0", "0", "0", "0")
        );

        assertThat(candle.confirmed()).isFalse();
    }
}
