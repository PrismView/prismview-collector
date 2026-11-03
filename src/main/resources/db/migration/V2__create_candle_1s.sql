-- 1초봉 원본: OKX history-candles(bar=1s)의 확정 봉(confirm=1)만 저장
-- 보관 정책(ADR-0005): 최근 3개월은 이 테이블에 월별 파티션으로 보관,
--   3개월이 지나면 월별 Parquet로 내보내고 행 수·ts 범위 검증 후 파티션 DROP
CREATE TABLE candle_1s (
    inst_id    VARCHAR(32)    NOT NULL,   -- 종목 (예: BTC-USDT-SWAP)
    ts         BIGINT         NOT NULL,   -- 봉 시작 시각, UTC epoch ms

    open       NUMERIC(18, 8) NOT NULL,
    high       NUMERIC(18, 8) NOT NULL,
    low        NUMERIC(18, 8) NOT NULL,
    close      NUMERIC(18, 8) NOT NULL,

    vol        NUMERIC(24, 8) NOT NULL,   -- 거래량: 계약 수
    vol_ccy    NUMERIC(24, 8) NOT NULL,   -- 거래량: 코인 수량 (BTC, ETH)
    vol_quote  NUMERIC(24, 8) NOT NULL,   -- 거래량: USDT 금액

    -- 파티션 키(ts)가 PK에 포함되어야 파티션별 유일성 검사로 전체 유일성이 보장됨
    CONSTRAINT pk_candle_1s PRIMARY KEY (inst_id, ts),

    -- 부모에 정의한 CHECK는 모든 파티션이 물려받음
    CONSTRAINT ck_candle_1s_ts_boundary CHECK (ts % 1000 = 0),                -- 초 경계에 정렬
    CONSTRAINT ck_candle_1s_price_pos   CHECK (low > 0),
    CONSTRAINT ck_candle_1s_hl          CHECK (high >= GREATEST(open, close)
                                           AND low  <= LEAST(open, close)),
    CONSTRAINT ck_candle_1s_vol_nonneg  CHECK (vol >= 0 AND vol_ccy >= 0 AND vol_quote >= 0)
) PARTITION BY RANGE (ts);

-- 월별 파티션 (UTC 기준, FROM 포함 / TO 미포함)
-- 시간대(+00)를 반드시 명시: 생략하면 세션 TimeZone에 따라 경계가 달라짐
-- DEFAULT 파티션은 만들지 않음: 범위 밖 ts는 INSERT 에러로 드러나게 함
CREATE TABLE candle_1s_2026_08 PARTITION OF candle_1s FOR VALUES
    FROM ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2026-08-01 00:00:00+00') * 1000)::BIGINT)
    TO   ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2026-09-01 00:00:00+00') * 1000)::BIGINT);

CREATE TABLE candle_1s_2026_09 PARTITION OF candle_1s FOR VALUES
    FROM ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2026-09-01 00:00:00+00') * 1000)::BIGINT)
    TO   ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2026-10-01 00:00:00+00') * 1000)::BIGINT);

CREATE TABLE candle_1s_2026_10 PARTITION OF candle_1s FOR VALUES
    FROM ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2026-10-01 00:00:00+00') * 1000)::BIGINT)
    TO   ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2026-11-01 00:00:00+00') * 1000)::BIGINT);

CREATE TABLE candle_1s_2026_11 PARTITION OF candle_1s FOR VALUES
    FROM ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2026-11-01 00:00:00+00') * 1000)::BIGINT)
    TO   ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2026-12-01 00:00:00+00') * 1000)::BIGINT);

CREATE TABLE candle_1s_2026_12 PARTITION OF candle_1s FOR VALUES
    FROM ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2026-12-01 00:00:00+00') * 1000)::BIGINT)
    TO   ((EXTRACT(EPOCH FROM TIMESTAMPTZ '2027-01-01 00:00:00+00') * 1000)::BIGINT);