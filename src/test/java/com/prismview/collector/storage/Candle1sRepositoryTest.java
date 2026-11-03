package com.prismview.collector.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prismview.collector.TestcontainersConfiguration;
import com.prismview.collector.domain.Candle;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class Candle1sRepositoryTest {

    private static final String BTC = "BTC-USDT-SWAP";
    private static final String ETH = "ETH-USDT-SWAP";
    private static final long TS = 1793685281000L; // 2026-11-03, candle_1s_2026_11 파티션 범위

    @Autowired
    Candle1sRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.execute("TRUNCATE candle_1s");
    }

    @Test
    void 같은_페이지를_두_번_저장해도_행_수는_그대로() {
        List<Candle> page = List.of(candle(BTC, TS, "100"), candle(BTC, TS + 1000, "101"));

        repository.upsertAll(page);
        repository.upsertAll(page);

        assertThat(count()).isEqualTo(2);
    }

    @Test
    void 같은_키에_값이_바뀌면_갱신된다() {
        repository.upsertAll(List.of(candle(BTC, TS, "100")));
        repository.upsertAll(List.of(candle(BTC, TS, "105")));

        BigDecimal close = jdbcTemplate.queryForObject(
                "SELECT close FROM candle_1s WHERE inst_id = ? AND ts = ?", BigDecimal.class, BTC, TS);
        assertThat(close).isEqualByComparingTo("105");
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void 빈_리스트는_아무것도_하지_않는다() {
        repository.upsertAll(List.of());

        assertThat(count()).isZero();
    }

    @Test
    void 확정되지_않은_봉이_섞이면_하나도_저장하지_않고_예외() {
        List<Candle> page = List.of(candle(BTC, TS, "100"), unconfirmed(BTC, TS + 1000));

        assertThatThrownBy(() -> repository.upsertAll(page))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(count()).isZero();
    }

    @Test
    void 데이터가_없으면_최소_ts는_빈_값() {
        assertThat(repository.findMinTs(BTC)).isEmpty();
    }

    @Test
    void 최소_ts는_종목별로_따로_계산된다() {
        repository.upsertAll(List.of(candle(BTC, TS, "100"), candle(BTC, TS + 1000, "101")));
        repository.upsertAll(List.of(candle(ETH, TS + 5000, "3000")));

        assertThat(repository.findMinTs(BTC)).contains(TS);
        assertThat(repository.findMinTs(ETH)).contains(TS + 5000);
    }

    // --- 테스트 데이터 도우미 ---

    private long count() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM candle_1s", Long.class);
    }

    /** o=h=l=c=price인 확정 봉 (Candle 검증과 DB CHECK를 모두 통과하는 가장 단순한 봉) */
    private static Candle candle(String instId, long ts, String price) {
        BigDecimal p = new BigDecimal(price);
        BigDecimal v = new BigDecimal("1");
        return new Candle(instId, ts, p, p, p, p, v, v, v, true);
    }

    private static Candle unconfirmed(String instId, long ts) {
        BigDecimal p = new BigDecimal("100");
        BigDecimal v = new BigDecimal("1");
        return new Candle(instId, ts, p, p, p, p, v, v, v, false);
    }
}