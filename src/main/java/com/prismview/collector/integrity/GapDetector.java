package com.prismview.collector.integrity;

import com.prismview.collector.domain.Timeframe;

import java.util.ArrayList;
import java.util.List;

/**
 * 이웃한 ts 간격이 봉 길이보다 크면 그 사이가 비어 있다.
 * 빠진 개수 = (차이 / durationMs) - 1.
 */
public final class GapDetector {

    private GapDetector() {
    }

    public static List<Gap> findGaps(Timeframe timeframe, List<Long> sortedTimestamps) {
        List<Gap> gaps = new ArrayList<>();
        long duration = timeframe.durationMs();
        for (int i = 1; i < sortedTimestamps.size(); i++) {
            long previous = sortedTimestamps.get(i - 1);
            long current = sortedTimestamps.get(i);
            long delta = current - previous;
            if (delta > duration) {
                long missing = delta / duration - 1;
                gaps.add(new Gap(previous, current, missing));
            }
        }
        return gaps;
    }

    public record Gap(long previousTs, long currentTs, long missingCount) {
    }
}
