package dev.videobridge;

import dev.videobridge.common.Times;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimesTest {

    @Test
    void datesSpanEachDay() {
        assertEquals(List.of("2026-06-16", "2026-06-17"),
                Times.dates("2026-06-16 23:00:00", "2026-06-17 01:00:00"));
    }

    @Test
    void mp4FileNameIsFilteredByTimeRange() {
        long from = Times.epochSeconds("2026-06-16 16:00:00");
        long to = Times.epochSeconds("2026-06-16 17:00:00");
        assertTrue(Times.inRange("2026-06-16-16-04-25-0.mp4", from, to));
        assertFalse(Times.inRange("2026-06-16-15-04-25-0.mp4", from, to));
        // 解析不了的文件名不拦，宁可多返回一个
        assertTrue(Times.inRange("weird.mp4", from, to));
    }

    @Test
    void badTimeFormatFailsClearly() {
        assertThrows(IllegalArgumentException.class, () -> Times.parse("2026/06/16"));
    }
}
