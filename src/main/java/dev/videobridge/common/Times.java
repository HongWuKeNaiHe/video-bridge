package dev.videobridge.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class Times {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Times() {
    }

    public static LocalDateTime parse(String time) {
        try {
            return LocalDateTime.parse(time, FORMAT);
        } catch (Exception e) {
            throw new IllegalArgumentException("时间格式应为 yyyy-MM-dd HH:mm:ss: " + time);
        }
    }

    public static String format(LocalDateTime time) {
        return time.format(FORMAT);
    }

    public static long epochSeconds(String time) {
        return parse(time).atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /** 按天展开，ZLM 的 MP4 文件是按日期目录存的 */
    public static List<String> dates(String startTime, String endTime) {
        LocalDate start = parse(startTime).toLocalDate();
        LocalDate end = parse(endTime).toLocalDate();
        List<String> dates = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            dates.add(day.toString());
        }
        return dates;
    }

    /** 文件名形如 2026-06-17-16-04-25-0.mp4，解析失败时不过滤 */
    public static boolean inRange(String fileName, long startEpoch, long endEpoch) {
        try {
            String stamp = fileName.substring(0, 19);
            LocalDateTime time = LocalDateTime.parse(
                    stamp.substring(0, 10) + " " + stamp.substring(11, 13) + ":"
                            + stamp.substring(14, 16) + ":" + stamp.substring(17, 19), FORMAT);
            long epoch = time.atZone(ZoneId.systemDefault()).toEpochSecond();
            return epoch >= startEpoch && epoch <= endEpoch;
        } catch (Exception e) {
            return true;
        }
    }
}
