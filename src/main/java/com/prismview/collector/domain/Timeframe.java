package com.prismview.collector.domain;

public enum Timeframe {
    S1("1s", 1_000L),
    S30("30s", 30_000L);

    private final String code;
    private final long durationMs;

    Timeframe(String code, long durationMs) {
        this.code = code;
        this.durationMs = durationMs;
    }

    public String code() {
        return code;
    }

    public long durationMs() {
        return durationMs;
    }

    public static Timeframe fromCode(String code) {
        for (Timeframe timeframe : values()) {
            if (timeframe.code.equals(code)) {
                return timeframe;
            }
        }
        throw new IllegalArgumentException("Unknown timeframe: " + code);
    }
}
