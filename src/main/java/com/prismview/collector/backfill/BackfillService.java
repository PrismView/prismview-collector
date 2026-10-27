package com.prismview.collector.backfill;

import com.prismview.collector.domain.Timeframe;
import com.prismview.collector.okx.OkxCandleClient;
import com.prismview.collector.storage.CandleRepository;
import org.springframework.stereotype.Service;

/**
 * 목표 시점까지 history-candles를 after 커서로 되감아 1초봉을 넣는다.
 * 커서는 응답의 가장 오래된 ts여야 한다. 페이지가 겹쳐도 upsert라 안전하다.
 */
@Service
public class BackfillService {

    private final OkxCandleClient okxCandleClient;
    private final CandleRepository candleRepository;

    public BackfillService(OkxCandleClient okxCandleClient, CandleRepository candleRepository) {
        this.okxCandleClient = okxCandleClient;
        this.candleRepository = candleRepository;
    }

    public void backfill(String instId, long targetStartTs) {
        throw new UnsupportedOperationException(
                "3개월 1초봉 백필은 다음 단계에서 구현. instId=" + instId
                        + ", targetStartTs=" + targetStartTs
                        + ", source=" + Timeframe.S1.code()
        );
    }
}
