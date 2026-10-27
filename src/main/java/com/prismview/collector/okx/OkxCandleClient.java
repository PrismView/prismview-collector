package com.prismview.collector.okx;

import com.prismview.collector.domain.Candle;
import com.prismview.collector.domain.Timeframe;

import java.util.List;

/**
 * OKX public REST. 과거 백필은 /api/v5/market/history-candles, after 커서는 응답의 가장 오래된 ts.
 */
public interface OkxCandleClient {

    List<Candle> historyCandles(String instId, Timeframe timeframe, Long afterTs, int limit);
}
