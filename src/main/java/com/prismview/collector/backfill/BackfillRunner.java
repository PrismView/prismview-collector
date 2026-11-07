package com.prismview.collector.backfill;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * backfill 프로필에서만 동작: 설정된 종목을 차례로 OKX 제공 범위 끝까지 백필한다.
 * 실행: ./gradlew bootRun --args='--spring.profiles.active=local,backfill'
 * 중간에 멈춰도 다시 실행하면 DB의 가장 오래된 ts부터 이어서 받는다.
 */
@Component
@Profile("backfill")
public class BackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BackfillRunner.class);

    private final BackfillService backfillService;
    private final List<String> instIds;

    public BackfillRunner(BackfillService backfillService,
                          @Value("${okx.backfill.inst-ids}") List<String> instIds) {
        this.backfillService = backfillService;
        this.instIds = instIds;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("백필 대상: {}", instIds);
        for (String instId : instIds) {
            long startNanos = System.nanoTime();
            BackfillService.Result result = backfillService.backfill(instId);
            long seconds = Duration.ofNanos(System.nanoTime() - startNanos).toSeconds();

            log.info("[{}] 백필 완료: 페이지={}, 저장={}, 가장 오래된 봉={}, 소요={}s",
                    instId, result.pages(), result.saved(),
                    result.oldestTs() == null ? "없음" : Instant.ofEpochMilli(result.oldestTs()),
                    seconds);
        }
        log.info("전체 백필 완료");
    }
}