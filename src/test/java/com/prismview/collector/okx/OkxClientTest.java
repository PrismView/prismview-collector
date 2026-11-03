package com.prismview.collector.okx;

import com.prismview.collector.domain.Candle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

class OkxClientTest {

    private static final String BASE_URL = "https://www.okx.com";
    private static final String PATH = "/api/v5/market/history-candles";
    private static final String INST_ID = "BTC-USDT-SWAP";

    private MockRestServiceServer server;
    private OkxClient okxClient;

    // 각 테스트 전에 새 가짜 서버와 클라이언트 준비 (pytest fixture와 같은 역할)
    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();   // build() 전에 연결해야 함
        okxClient = new OkxClient(builder, BASE_URL);
    }

    @Test
    void 정상_응답이면_Candle_100개_반환() {
        server.expect(requestTo(startsWith(BASE_URL + PATH)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("instId", INST_ID))
                .andExpect(queryParam("bar", "1s"))
                .andExpect(queryParam("limit", "100"))
                .andRespond(withSuccess(
                        new ClassPathResource("okx/history-candles-1s.json"),
                        MediaType.APPLICATION_JSON));

        List<Candle> candles = okxClient.fetchOnePage(INST_ID, null);

        assertEquals(100, candles.size());
        assertEquals(INST_ID, candles.get(0).instId());
        server.verify();   // 기대한 요청이 실제로 왔는지
    }

    @Test
    void after가_있으면_쿼리에_포함() {
        server.expect(requestTo(startsWith(BASE_URL + PATH)))
                .andExpect(queryParam("after", "1793685281000"))
                .andRespond(withSuccess(
                        new ClassPathResource("okx/history-candles-1s.json"),
                        MediaType.APPLICATION_JSON));

        okxClient.fetchOnePage(INST_ID, 1793685281000L);

        server.verify();
    }

    @Test
    void code가_0이_아니면_OkxApiException() {
        String errorBody = """
                {"code":"51001","msg":"Instrument ID does not exist","data":[]}
                """;
        server.expect(requestTo(startsWith(BASE_URL + PATH)))
                .andRespond(withSuccess(errorBody, MediaType.APPLICATION_JSON));   // HTTP 200!

        OkxApiException e = assertThrows(OkxApiException.class,
                () -> okxClient.fetchOnePage(INST_ID, null));

        assertEquals("51001", e.code());
    }

    @Test
    void HTTP_429면_TooManyRequests_예외() {
        server.expect(requestTo(startsWith(BASE_URL + PATH)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(HttpClientErrorException.TooManyRequests.class,
                () -> okxClient.fetchOnePage(INST_ID, null));
    }

    @Test
    void HTTP_500이면_서버_오류_예외() {
        server.expect(requestTo(startsWith(BASE_URL + PATH)))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class,
                () -> okxClient.fetchOnePage(INST_ID, null));
    }
}