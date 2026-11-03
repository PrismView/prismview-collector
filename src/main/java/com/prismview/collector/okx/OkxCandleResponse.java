package com.prismview.collector.okx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * OKX GET /api/v5/market/history-candles 응답 그대로의 모양 (값은 전부 문자열).
 * data의 각 행: [ts, o, h, l, c, vol, volCcy, volCcyQuote, confirm]
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OkxCandleResponse(
        String code,
        String msg,
        List<List<String>> data
) {
}