# prismview-collector

OKX USDT 무기한 선물 BTC·ETH의 **1초봉**을 모아 **30초봉**으로 집계하는 Java 수집기입니다.

> 1단계 뼈대입니다. OKX 호출·3개월 백필·스케줄 증분은 다음 커밋에서 채웁니다.

설계 이유: [ADR](https://github.com/PrismView/PrismView/tree/main/docs/adr)

## 하는 일

| 단계 | 내용 |
| --- | --- |
| 수집 | `history-candles`로 확정된 1초봉 백필 (1차 범위 3개월) |
| 저장 | PostgreSQL `candles`에 1초봉·30초봉을 누적 upsert |
| 집계 | 1초봉 30개 → 30초봉 (모자란 버킷은 만들지 않음) |
| 검사 | ts 간격으로 공백을 찾고, 재요청 후에도 비면 `known_gaps` |
| 내보내기 | 백테스터용 Parquet (예정) |

실시간 WebSocket은 이번 단계에 넣지 않습니다.

## 실행

JDK 21과 Docker가 필요합니다.

```bash
docker compose up -d
./gradlew test
./gradlew bootRun
```

PostgreSQL은 `localhost:5432`, DB/유저/비밀번호는 `prismview`입니다. Flyway가 기동 시 `V1__create_candles.sql`을 적용합니다.

## 구조

```
src/main/java/com/prismview/collector/
 ├─ domain/       Candle, Timeframe
 ├─ okx/          OKX REST 호출
 ├─ storage/      JdbcTemplate 배치 upsert
 ├─ backfill/     3개월 과거 적재
 ├─ aggregate/    1초봉 → 30초봉
 └─ integrity/    누락 검사
```

## 스택

Spring Boot 3.5.16 · Java 21 · Gradle · JdbcTemplate · Flyway · PostgreSQL 16 · Testcontainers
