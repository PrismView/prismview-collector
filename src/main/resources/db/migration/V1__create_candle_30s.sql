-- 30초봉: OKX 1초봉(확정 봉)을 UTC :00/:30 경계로 집계한 결과
CREATE TABLE candle_30s (
    inst_id    VARCHAR(32)    NOT NULL,   -- 종목 (예: BTC-USDT-SWAP)
    ts         BIGINT         NOT NULL,   -- 봉 시작 시각, UTC epoch ms

    open       NUMERIC(18, 8) NOT NULL,   -- 버킷 첫 1초봉의 시가
    high       NUMERIC(18, 8) NOT NULL,   -- 버킷 내 최고가
    low        NUMERIC(18, 8) NOT NULL,   -- 버킷 내 최저가
    close      NUMERIC(18, 8) NOT NULL,   -- 버킷 마지막 1초봉의 종가

    vol        NUMERIC(24, 8) NOT NULL,   -- 거래량: 계약 수
    vol_ccy    NUMERIC(24, 8) NOT NULL,   -- 거래량: 코인 수량 (BTC, ETH)
    vol_quote  NUMERIC(24, 8) NOT NULL,   -- 거래량: USDT 금액

    src_cnt    SMALLINT       NOT NULL,   -- 집계에 쓰인 1초봉 개수 (30이면 완전한 버킷)

    -- 같은 종목·같은 시각의 봉은 하나만 → 재수집해도 upsert로 한 행 유지
    CONSTRAINT pk_candle_30s PRIMARY KEY (inst_id, ts),

    -- 값 검증: 코드에 버그가 있어도 DB가 마지막으로 막는다
    CONSTRAINT ck_candle_30s_ts_boundary CHECK (ts % 30000 = 0),               -- 30초 경계에 정렬
    CONSTRAINT ck_candle_30s_src_cnt     CHECK (src_cnt BETWEEN 1 AND 30),
    CONSTRAINT ck_candle_30s_price_pos   CHECK (low > 0),                      -- 가격은 양수
    CONSTRAINT ck_candle_30s_hl          CHECK (high >= GREATEST(open, close)
                                            AND low  <= LEAST(open, close)),   -- 고가·저가 관계
    CONSTRAINT ck_candle_30s_vol_nonneg  CHECK (vol >= 0 AND vol_ccy >= 0 AND vol_quote >= 0)
);