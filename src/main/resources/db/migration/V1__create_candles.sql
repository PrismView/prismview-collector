CREATE TABLE candles (
    inst_id     VARCHAR(32)    NOT NULL,
    timeframe   VARCHAR(8)     NOT NULL,
    ts          BIGINT         NOT NULL,
    open        NUMERIC(18, 8) NOT NULL,
    high        NUMERIC(18, 8) NOT NULL,
    low         NUMERIC(18, 8) NOT NULL,
    close       NUMERIC(18, 8) NOT NULL,
    vol         NUMERIC(24, 8) NOT NULL,
    vol_ccy     NUMERIC(24, 8) NOT NULL,
    vol_quote   NUMERIC(24, 8) NOT NULL,
    confirmed   BOOLEAN        NOT NULL DEFAULT TRUE,
    PRIMARY KEY (inst_id, timeframe, ts)
);

CREATE TABLE known_gaps (
    inst_id    VARCHAR(32) NOT NULL,
    timeframe  VARCHAR(8)  NOT NULL,
    from_ts    BIGINT      NOT NULL,
    to_ts      BIGINT      NOT NULL,
    reason     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (inst_id, timeframe, from_ts, to_ts)
);
