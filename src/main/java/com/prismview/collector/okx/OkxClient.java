package com.prismview.collector.okx;

import com.prismview.collector.domain.Candle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class OkxClient {

    private static final String HISTORY_CANDLES_PATH = "/api/v5/market/history-candles";
    private static final String BAR = "1s";
    private static final int LIMIT = 100;

    private final RestClient restClient;

    // Spring이 RestClient.Builder와 설정값을 넣어 줌 (FastAPI의 Depends와 비슷)
    public OkxClient(RestClient.Builder builder, @Value("${okx.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * 1초봉 한 페이지(최대 100개, 최신순).
     * @param after 이 ts(ms)보다 이전 봉을 요청. null이면 최신부터. (페이지네이션은 M3)
     */
    public List<Candle> fetchOnePage(String instId, Long after) {
        OkxCandleResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(HISTORY_CANDLES_PATH)
                        .queryParam("instId", instId)
                        .queryParam("bar", BAR)
                        .queryParam("limit", LIMIT)
                        .queryParamIfPresent("after", Optional.ofNullable(after))
                        .build())
                .retrieve()
                .body(OkxCandleResponse.class);

        if (response == null) {
            throw new OkxApiException("EMPTY", "응답 body 없음");
        }
        if (!"0".equals(response.code())) {
            throw new OkxApiException(response.code(), response.msg());
        }
        return OkxCandleMapper.toCandles(instId, response);
    }
}