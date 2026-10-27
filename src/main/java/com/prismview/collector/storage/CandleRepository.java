package com.prismview.collector.storage;

import com.prismview.collector.domain.Candle;
import com.prismview.collector.domain.Timeframe;

import java.util.List;
import java.util.Optional;

public interface CandleRepository {

    void upsertAll(List<Candle> candles);

    Optional<Long> findLatestTs(String instId, Timeframe timeframe);
}
