package com.prismview.collector.backfill;

import com.prismview.collector.domain.Candle;
import java.util.List;

/**
 * 1초봉을 한 페이지씩 돌려주는 출처.
 * 실제 구현은 OkxClient, 테스트에서는 미리 정한 페이지를 돌려주는 가짜로 바꿔 끼운다.
 */
public interface CandlePageSource {

    /** after보다 오래된 봉을 최신순으로 한 페이지. after가 null이면 최신부터. 더 없으면 빈 리스트. */
    List<Candle> fetchOnePage(String instId, Long after);
}