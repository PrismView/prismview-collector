package com.prismview.collector.integrity;

import com.prismview.collector.domain.Timeframe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GapDetectorTest {

    @Test
    void countsMissingBarsBetweenNeighbors() {
        List<Long> timestamps = List.of(0L, 1_000L, 7_000L);

        List<GapDetector.Gap> gaps = GapDetector.findGaps(Timeframe.S1, timestamps);

        assertThat(gaps).hasSize(1);
        assertThat(gaps.getFirst().missingCount()).isEqualTo(5);
    }
}
