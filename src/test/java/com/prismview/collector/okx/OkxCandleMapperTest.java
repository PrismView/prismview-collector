package com.prismview.collector.okx;

import com.prismview.collector.domain.Candle;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OkxCandleMapperTest {

    private static final String INST_ID = "BTC-USDT-SWAP";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void 샘플_3행을_값까지_정확히_변환() {
        // 2026-11-03 실제로 받은 응답 (limit=3)
        String json = """
                {"code":"0","data":[
                  ["1793685282000","76720.1","76720.1","76720.1","76720.1","0","0","0","0"],
                  ["1793685281000","76721.3","76721.3","76720.1","76720.1","174.25","1.7425","133686.60868","1"],
                  ["1793685280000","76721.4","76721.4","76721.4","76721.4","0.41","0.0041","314.55774","1"]
                ],"msg":""}
                """;

        OkxCandleResponse response = jsonMapper.readValue(json, OkxCandleResponse.class);
        List<Candle> candles = OkxCandleMapper.toCandles(INST_ID, response);

        assertEquals(3, candles.size());

        // 첫 행: 진행 중, 거래량 0
        Candle first = candles.get(0);
        assertEquals(1793685282000L, first.ts());
        assertFalse(first.confirm());
        assertEquals(0, first.vol().compareTo(BigDecimal.ZERO));

        // 둘째 행: 위치별로 올바른 필드에 들어갔는지
        Candle second = candles.get(1);
        assertEquals(INST_ID, second.instId());
        assertTrue(second.confirm());
        assertEquals(0, second.open().compareTo(new BigDecimal("76721.3")));
        assertEquals(0, second.low().compareTo(new BigDecimal("76720.1")));
        assertEquals(0, second.vol().compareTo(new BigDecimal("174.25")));
        assertEquals(0, second.volCcy().compareTo(new BigDecimal("1.7425")));
        assertEquals(0, second.volQuote().compareTo(new BigDecimal("133686.60868")));
    }

    @Test
    void fixture_100행은_최신순이고_1초_간격() {
        OkxCandleResponse response = jsonMapper.readValue(
                getClass().getResourceAsStream("/okx/history-candles-1s.json"),
                OkxCandleResponse.class);

        List<Candle> candles = OkxCandleMapper.toCandles(INST_ID, response);

        assertEquals("0", response.code());
        assertEquals(100, candles.size());
        for (int i = 1; i < candles.size(); i++) {
            assertEquals(1000L, candles.get(i - 1).ts() - candles.get(i).ts());
        }
    }

    @Test
    void 행의_값이_부족하면_예외() {
        OkxCandleResponse response = new OkxCandleResponse("0", "",
                List.of(List.of("1793685281000", "76721.3")));

        assertThrows(IllegalArgumentException.class,
                () -> OkxCandleMapper.toCandles(INST_ID, response));
    }
}