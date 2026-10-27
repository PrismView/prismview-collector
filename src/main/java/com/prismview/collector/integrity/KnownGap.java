package com.prismview.collector.integrity;

import com.prismview.collector.domain.Timeframe;

public record KnownGap(String instId, Timeframe timeframe, long fromTs, long toTs, String reason) {
}
