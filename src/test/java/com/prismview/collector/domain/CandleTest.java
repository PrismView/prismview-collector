package com.prismview.collector.domain;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CandleTest {
    private static final String INST_ID = "BTC-USDT-SWAP";
    private static final long TS = 1793685281000L;

    private static Candle validCandle() {
        return new Candle(
                INST_ID, TS,
                bd("76721.3"), bd("76721.3"), bd("76720.1"), bd("76720.1"),
                bd("174.25"), bd("1.7425"), bd("133686.60868"),
                true
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void 정상_생성() {
        Candle candle = validCandle();

        assertEquals(INST_ID, candle.instId());
        assertEquals(TS, candle.ts());
        assertEquals(0, candle.close().compareTo(bd("76720.1")));
    }

    @Test
    void 체결_없는_초는_거래량0_ohlc_동일해도_허용() {
        BigDecimal p = bd("76720.1");

        Candle candle = new Candle(INST_ID, TS, p, p, p, p,
                bd("0"), bd("0"), bd("0"), false);

        assertEquals(0, candle.high().compareTo(candle.low()));
    }

    @Test
    void close가_null이면_예외() {
        assertThrows(NullPointerException.class, () -> new Candle(
                INST_ID, TS,
                bd("76721.3"), bd("76721.3"), bd("76720.1"), null,
                bd("174.25"), bd("1.7425"), bd("133686.60868"), true));
    }

    @Test
    void vol이_음수면_예외() {
        assertThrows(IllegalArgumentException.class, () -> new Candle(
                INST_ID, TS,
                bd("76721.3"), bd("76721.3"), bd("76720.1"), bd("76720.1"),
                bd("-1"), bd("1.7425"), bd("133686.60868"), true));
    }

    @Test
    void high가_low보다_작으면_예외() {
        assertThrows(IllegalArgumentException.class, () -> new Candle(
                INST_ID, TS,
                bd("76720.1"), bd("1"), bd("2"), bd("76720.1"),
                bd("174.25"), bd("1.7425"), bd("133686.60868"), true));
    }

    @Test
    void ts가_0이면_예외() {
        assertThrows(IllegalArgumentException.class, () -> new Candle(
                INST_ID, 0L,
                bd("76721.3"), bd("76721.3"), bd("76720.1"), bd("76720.1"),
                bd("174.25"), bd("1.7425"), bd("133686.60868"), true));
    }
}