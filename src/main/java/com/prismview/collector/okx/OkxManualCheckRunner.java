package com.prismview.collector.okx;

import com.prismview.collector.domain.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 실제 OKX에 한 번 호출해 결과를 로그로 확인.
 * okx-check 프로필을 켰을 때만 실행된다.
 */
@Component
@Profile("okx-check")
public class OkxManualCheckRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OkxManualCheckRunner.class);

    private final OkxClient okxClient;

    public OkxManualCheckRunner(OkxClient okxClient) {
        this.okxClient = okxClient;
    }

    @Override
    public void run(String... args) {
        List<Candle> candles = okxClient.fetchOnePage("BTC-USDT-SWAP", null);

        Candle newest = candles.get(0);
        Candle oldest = candles.get(candles.size() - 1);
        log.info("받은 봉: {}개", candles.size());
        log.info("최신: {} {}", Instant.ofEpochMilli(newest.ts()), newest);
        log.info("가장 오래된: {} {}", Instant.ofEpochMilli(oldest.ts()), oldest);
    }
}