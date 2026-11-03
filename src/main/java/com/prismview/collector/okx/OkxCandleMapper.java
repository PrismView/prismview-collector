package com.prismview.collector.okx;

import com.prismview.collector.domain.Candle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * OKX 캔들 응답(DTO) → 도메인 Candle 변환.
 */
public final class OkxCandleMapper {

    // OKX 캔들 행의 위치: [ts, o, h, l, c, vol, volCcy, volCcyQuote, confirm]
    private static final int TS = 0;
    private static final int OPEN = 1;
    private static final int HIGH = 2;
    private static final int LOW = 3;
    private static final int CLOSE = 4;
    private static final int VOL = 5;
    private static final int VOL_CCY = 6;
    private static final int VOL_CCY_QUOTE = 7;
    private static final int CONFIRM = 8;
    private static final int MIN_ROW_SIZE = 9;

    // 객체를 만들 필요 없는 도구 클래스라 생성자를 막아 둠
    private OkxCandleMapper() {
    }

    public static List<Candle> toCandles(String instId, OkxCandleResponse response) {
        List<Candle> candles = new ArrayList<>();
        for (List<String> row : response.data()) {
            candles.add(toCandle(instId, row));
        }
        return candles;
    }

    private static Candle toCandle(String instId, List<String> row) {
        if (row.size() < MIN_ROW_SIZE) {
            throw new IllegalArgumentException(
                    "OKX 캔들 행의 값이 부족함: 기대 " + MIN_ROW_SIZE + "개, 실제 " + row.size() + "개 " + row);
        }
        return new Candle(
                instId,                                      // 응답엔 없고 요청에서 알고 있는 값
                Long.parseLong(row.get(TS)),
                new BigDecimal(row.get(OPEN)),
                new BigDecimal(row.get(HIGH)),
                new BigDecimal(row.get(LOW)),
                new BigDecimal(row.get(CLOSE)),
                new BigDecimal(row.get(VOL)),
                new BigDecimal(row.get(VOL_CCY)),
                new BigDecimal(row.get(VOL_CCY_QUOTE)),      // OKX volCcyQuote → volQuote
                "1".equals(row.get(CONFIRM))                 // "1" 확정, "0" 진행 중
        );
    }
}