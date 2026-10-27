package com.prismview.collector.okx;

import com.prismview.collector.domain.Candle;
import com.prismview.collector.domain.Timeframe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class RestOkxCandleClient implements OkxCandleClient {

    private final RestClient restClient;
    private final String historyPath;
    private final int defaultLimit;

    public RestOkxCandleClient(
            @Value("${prismview.collector.okx.base-url}") String baseUrl,
            @Value("${prismview.collector.okx.history-path}") String historyPath,
            @Value("${prismview.collector.okx.limit}") int defaultLimit
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
        this.historyPath = historyPath;
        this.defaultLimit = defaultLimit;
    }

    @Override
    public List<Candle> historyCandles(String instId, Timeframe timeframe, Long afterTs, int limit) {
        // 1단계에서 페이지네이션·레이트 리밋·confirm 필터를 붙인다.
        throw new UnsupportedOperationException("OKX history-candles 호출은 다음 단계에서 구현");
    }

    int defaultLimit() {
        return defaultLimit;
    }

    String historyPath() {
        return historyPath;
    }

    RestClient restClient() {
        return restClient;
    }
}
