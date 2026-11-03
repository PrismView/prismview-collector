package com.prismview.collector.domain; // ① 이 파일의 위치 (폴더 경로와 같아야 함)

import java.math.BigDecimal; // ② 다른 패키지의 클래스 가져오기
import java.util.Objects;

/* 🤔 질문: Candle.java 파일 안에 public record Bar(...)라고 쓰면 어떻게 될까?
해설: 컴파일 에러가 난다. Java는 public 타입의 이름과 파일 이름이 같아야 한다. Python은 파일 하나에 클래스를 몇 개든 아무 이름으로 둘 수 있지만, Java는 "파일 하나 = public 타입 하나"가 원칙이다. */

// 필드 이름은 DB 스키마(candle_30s)의 컬럼명과 맞춘다 (snake_case → camelCase)
public record Candle( // ③ 타입 선언 (파일 이름 = 타입 이름) public는 "다른 패키지에서도 쓸 수 있다는 뜻"
    String instId,      // inst_id
    long ts,            // ts: 봉 시작 시각, UTC epoch ms
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal vol,     // 거래량: 계약 수
    BigDecimal volCcy,  // vol_ccy: 거래량: 코인 수량
    BigDecimal volQuote, // vol_quote: 거래대금: USDT (OKX 응답의 volCcyQuote)
    boolean confirm     // OKX 확정 여부 (DB에는 저장 안 함)
) {

    // compact constructor : Python의 __post_init__
    public Candle {
        // 1. null 검사 (long, boolean은 null 불가라 제외)
        Objects.requireNonNull(instId, "instId는 필수");
        Objects.requireNonNull(open, "open은 필수");
        Objects.requireNonNull(high, "high는 필수");
        Objects.requireNonNull(low, "low는 필수");
        Objects.requireNonNull(close, "close는 필수");
        Objects.requireNonNull(vol, "vol은 필수");
        Objects.requireNonNull(volCcy, "volCcy는 필수");
        Objects.requireNonNull(volQuote, "volQuote는 필수");

        // 2. ts는 양수
        if (ts <= 0) {
            throw new IllegalArgumentException("ts는 양수여야 함: " + ts);
        }

        // 3. 음수 금지 (0 허용: 체결 없는 초는 거래량 0)
        requireNonNegative(open, "open");
        requireNonNegative(high, "high");
        requireNonNegative(low, "low");
        requireNonNegative(close, "close");
        requireNonNegative(vol, "vol");
        requireNonNegative(volCcy, "volCcy");
        requireNonNegative(volQuote, "volQuote");

        // 4. high >= low (같을 수 있음 : 체결 없는 초는 o=h=l=c)
        if (high.compareTo(low) < 0) {
            throw new IllegalArgumentException("high < low: high=" + high + ", low=" + low);
        }
    }

    // 이 record 안에서만 쓰는 도우미 (Python의 모듈 수준 _helper 함수)
    private static void requireNonNegative(BigDecimal value, String name) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + "는 음수 불가: " + value);
        }
    }
}
