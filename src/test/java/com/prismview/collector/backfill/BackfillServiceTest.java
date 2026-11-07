package com.prismview.collector.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prismview.collector.TestcontainersConfiguration;
import com.prismview.collector.domain.Candle;
import com.prismview.collector.ratelimit.MinIntervalRateLimiter; // 추가
import com.prismview.collector.storage.Candle1sRepository;
import java.math.BigDecimal;
import java.time.Duration; // 추가
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BackfillServiceTest {

    private static final String BTC = "BTC-USDT-SWAP";
    private static final long TS = 1793685281000L; // 2026-11-03, candle_1s_2026_11 파티션 범위

    /** 서비스 테스트는 커서 흐름만 본다 → 속도 제한 없음 */
    private static final MinIntervalRateLimiter NO_LIMIT = new MinIntervalRateLimiter(Duration.ZERO); // 추가

    @Autowired
    Candle1sRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.execute("TRUNCATE candle_1s");
    }

    @Test
    void 빈_DB면_최신부터_시작해_빈_응답까지_받는다() {
        FakeSource source = new FakeSource(
                page(TS, 3),            // TS, TS-1s, TS-2s
                page(TS - 3000, 3));    // TS-3s, TS-4s, TS-5s → 다음은 빈 응답
        BackfillService service = new BackfillService(source, repository, NO_LIMIT); // 수정 1

        BackfillService.Result result = service.backfill(BTC);

        // 커서: null(최신부터) → 1페이지의 가장 오래된 ts → 2페이지의 가장 오래된 ts
        assertThat(source.afters).containsExactly(null, TS - 2000, TS - 5000);
        assertThat(result.pages()).isEqualTo(2);
        assertThat(result.saved()).isEqualTo(6);
        assertThat(result.oldestTs()).isEqualTo(TS - 5000);
        assertThat(count()).isEqualTo(6);
    }

    @Test
    void 진행_중인_봉은_저장하지_않지만_커서_계산에는_포함된다() {
        FakeSource source = new FakeSource(List.of(
                candle(TS, false),          // 맨 앞 = 진행 중인 봉
                candle(TS - 1000, true),
                candle(TS - 2000, true)));
        BackfillService service = new BackfillService(source, repository, NO_LIMIT); // 수정 2

        BackfillService.Result result = service.backfill(BTC);

        assertThat(result.saved()).isEqualTo(2);
        assertThat(count()).isEqualTo(2);
        assertThat(source.afters).containsExactly(null, TS - 2000);
    }

    @Test
    void 커서가_줄어들지_않으면_예외를_던지고_그_페이지는_저장하지_않는다() {
        FakeSource source = new FakeSource(
                page(TS, 3),
                page(TS, 3));           // 같은 페이지를 또 줌 → 커서가 줄지 않음
        BackfillService service = new BackfillService(source, repository, NO_LIMIT); // 수정 3

        assertThatThrownBy(() -> service.backfill(BTC))
                .isInstanceOf(IllegalStateException.class);
        assertThat(count()).isEqualTo(3); // 첫 페이지만 저장됨
    }

    @Test
    void DB에_데이터가_있으면_가장_오래된_ts부터_이어서_받는다() {
        repository.upsertAll(page(TS, 3)); // 이전 실행에서 TS ~ TS-2s까지 받아 둔 상태
        FakeSource source = new FakeSource();
        BackfillService service = new BackfillService(source, repository, NO_LIMIT); // 수정 4

        BackfillService.Result result = service.backfill(BTC);

        assertThat(source.afters).containsExactly(TS - 2000);
        assertThat(result.pages()).isZero();
        assertThat(result.oldestTs()).isEqualTo(TS - 2000);
    }

    @Test
    void 처음부터_빈_응답이면_아무것도_저장하지_않는다() {
        BackfillService service = new BackfillService(new FakeSource(), repository, NO_LIMIT); // 수정 5

        BackfillService.Result result = service.backfill(BTC);

        assertThat(result.pages()).isZero();
        assertThat(result.saved()).isZero();
        assertThat(result.oldestTs()).isNull();
        assertThat(count()).isZero();
    }

    // --- 가짜 출처: 미리 넣어 둔 페이지를 순서대로 돌려주고, 받은 after 값을 기록 ---

    static class FakeSource implements CandlePageSource {

        private final Deque<List<Candle>> pages = new ArrayDeque<>();
        final List<Long> afters = new ArrayList<>();

        @SafeVarargs
        FakeSource(List<Candle>... pages) {
            this.pages.addAll(List.of(pages));
        }

        @Override
        public List<Candle> fetchOnePage(String instId, Long after) {
            afters.add(after);
            return pages.isEmpty() ? List.of() : pages.poll();
        }
    }

    // --- 테스트 데이터 도우미 ---

    private long count() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM candle_1s", Long.class);
    }

    /** newest부터 1초씩 과거로 n개, 최신순 (OKX 응답과 같은 순서), 모두 확정 봉 */
    private static List<Candle> page(long newest, int n) {
        List<Candle> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(candle(newest - i * 1000L, true));
        }
        return result;
    }

    private static Candle candle(long ts, boolean confirm) {
        BigDecimal p = new BigDecimal("100");
        BigDecimal v = new BigDecimal("1");
        return new Candle(BTC, ts, p, p, p, p, v, v, v, confirm);
    }
}