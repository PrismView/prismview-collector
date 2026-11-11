package com.prismview.collector.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prismview.collector.TestcontainersConfiguration;
import com.prismview.collector.domain.Candle;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class Candle30sRepositoryTest {

    private static final String BTC = "BTC-USDT-SWAP";
    private static final String ETH = "ETH-USDT-SWAP";
    private static final long B0 = 1777593600000L; // 2026-05-01 00:00:00 UTC, 30초 경계
    private static final long BUCKET = 30_000L;

    @Autowired
    Candle30sRepository repository;

    @Autowired
    Candle1sRepository candle1sRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE candle_1s, candle_30s");
    }

    @Test
    void 일초봉_30개가_30초봉_하나로_집계된다() {
        // 가격 100~129, 10초에 고가 200, 20초에 저가 50 → 시가·종가와 고가·저가가 서로 다른 봉에서 나온다
        List<Candle> bucket = fullBucket(BTC, B0);
        bucket.set(10, candle(BTC, B0 + 10_000, "110", "200", "110", "110"));
        bucket.set(20, candle(BTC, B0 + 20_000, "120", "120", "50", "120"));
        candle1sRepository.upsertAll(bucket);

        int processed = repository.aggregate(BTC, B0, B0 + BUCKET);

        assertThat(processed).isEqualTo(1);
        Map<String, Object> row = row30s(BTC, B0);
        assertThat(dec(row, "open")).isEqualByComparingTo("100");
        assertThat(dec(row, "close")).isEqualByComparingTo("129");
        assertThat(dec(row, "high")).isEqualByComparingTo("200");
        assertThat(dec(row, "low")).isEqualByComparingTo("50");
        assertThat(dec(row, "vol")).isEqualByComparingTo("30");
        assertThat(dec(row, "vol_ccy")).isEqualByComparingTo("30");
        assertThat(dec(row, "vol_quote")).isEqualByComparingTo("30");
        assertThat(srcCnt(row)).isEqualTo(30);
    }

    @Test
    void 일초봉이_29개인_버킷도_버리지_않고_src_cnt_29로_저장한다() {
        List<Candle> bucket = fullBucket(BTC, B0);
        bucket.remove(15); // 15초 봉 누락
        candle1sRepository.upsertAll(bucket);

        repository.aggregate(BTC, B0, B0 + BUCKET);

        assertThat(count30s()).isEqualTo(1);
        assertThat(srcCnt(row30s(BTC, B0))).isEqualTo(29);
    }

    @Test
    void ts_역순으로_저장해도_시가와_종가는_시간순으로_정해진다() {
        List<Candle> bucket = fullBucket(BTC, B0);
        Collections.reverse(bucket); // 29초 봉이 가장 먼저 저장됨
        candle1sRepository.upsertAll(bucket);

        repository.aggregate(BTC, B0, B0 + BUCKET);

        Map<String, Object> row = row30s(BTC, B0);
        assertThat(dec(row, "open")).isEqualByComparingTo("100");
        assertThat(dec(row, "close")).isEqualByComparingTo("129");
    }

    @Test
    void 두_번_집계해도_행_수와_값이_같다() {
        candle1sRepository.upsertAll(fullBucket(BTC, B0));

        repository.aggregate(BTC, B0, B0 + BUCKET);
        Map<String, Object> first = row30s(BTC, B0);
        repository.aggregate(BTC, B0, B0 + BUCKET);

        assertThat(count30s()).isEqualTo(1);
        assertThat(row30s(BTC, B0)).isEqualTo(first);
    }

    @Test
    void 범위_밖의_일초봉은_포함되지_않는다() {
        candle1sRepository.upsertAll(List.of(flat(BTC, B0 - 1000, "999")));    // 범위 바로 앞
        candle1sRepository.upsertAll(fullBucket(BTC, B0));                       // 범위 안
        candle1sRepository.upsertAll(fullBucket(BTC, B0 + BUCKET));              // 범위 바로 뒤

        repository.aggregate(BTC, B0, B0 + BUCKET);

        assertThat(count30s()).isEqualTo(1);
        assertThat(srcCnt(row30s(BTC, B0))).isEqualTo(30);
    }

    @Test
    void 다른_종목의_일초봉은_섞이지_않는다() {
        candle1sRepository.upsertAll(fullBucket(BTC, B0));
        candle1sRepository.upsertAll(fullBucket(ETH, B0));

        repository.aggregate(BTC, B0, B0 + BUCKET);

        assertThat(count30s()).isEqualTo(1);
        assertThat(srcCnt(row30s(BTC, B0))).isEqualTo(30);
    }

    @Test
    void 집계된_src_cnt의_합은_범위_안_일초봉_개수와_같다() {
        candle1sRepository.upsertAll(fullBucket(BTC, B0));
        List<Candle> partial = fullBucket(BTC, B0 + BUCKET);
        partial.remove(0);
        candle1sRepository.upsertAll(partial);
        candle1sRepository.upsertAll(fullBucket(BTC, B0 + 2 * BUCKET));

        int processed = repository.aggregate(BTC, B0, B0 + 3 * BUCKET);

        assertThat(processed).isEqualTo(3);
        Long srcSum = jdbcTemplate.queryForObject("SELECT sum(src_cnt) FROM candle_30s", Long.class);
        Long count1s = jdbcTemplate.queryForObject("SELECT count(*) FROM candle_1s", Long.class);
        assertThat(srcSum).isEqualTo(count1s).isEqualTo(89L);
    }

    @Test
    void 범위가_30초_경계가_아니면_예외이고_아무것도_저장하지_않는다() {
        candle1sRepository.upsertAll(fullBucket(BTC, B0));

        assertThatThrownBy(() -> repository.aggregate(BTC, B0 + 1000, B0 + BUCKET))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.aggregate(BTC, B0, B0 + BUCKET + 1000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(count30s()).isZero();
    }

    @Test
    void from이_to보다_작지_않으면_예외() {
        assertThatThrownBy(() -> repository.aggregate(BTC, B0, B0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.aggregate(BTC, B0 + BUCKET, B0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- 테스트 데이터 도우미 ---

    private long count30s() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM candle_30s", Long.class);
    }

    private Map<String, Object> row30s(String instId, long ts) {
        return jdbcTemplate.queryForMap(
                "SELECT open, high, low, close, vol, vol_ccy, vol_quote, src_cnt "
                        + "FROM candle_30s WHERE inst_id = ? AND ts = ?",
                instId, ts);
    }

    private static BigDecimal dec(Map<String, Object> row, String column) {
        return (BigDecimal) row.get(column);
    }

    private static int srcCnt(Map<String, Object> row) {
        return ((Number) row.get("src_cnt")).intValue();
    }

    /** 0~29초 1초봉 30개, i초 봉의 가격은 100+i (o=h=l=c) */
    private static List<Candle> fullBucket(String instId, long bucketTs) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            candles.add(flat(instId, bucketTs + i * 1000L, String.valueOf(100 + i)));
        }
        return candles;
    }

    private static Candle flat(String instId, long ts, String price) {
        return candle(instId, ts, price, price, price, price);
    }

    private static Candle candle(String instId, long ts, String open, String high, String low, String close) {
        BigDecimal v = new BigDecimal("1");
        return new Candle(instId, ts,
                new BigDecimal(open), new BigDecimal(high), new BigDecimal(low), new BigDecimal(close),
                v, v, v, true);
    }
}