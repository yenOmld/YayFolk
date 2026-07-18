package com.yayfolk.backend.agent.query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

/**
 * 时间表达式解析工具。
 * 将"今天""明天""下周六""本周""周末"等相对时间解析为绝对日期。
 */
public class TimeResolver {

    private TimeResolver() {
        // 工具类
    }

    /**
     * 解析用户输入中的相对时间表达，返回可读的时间上下文描述。
     */
    public static String resolveRelativeTime(String userInput) {
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();

        if (userInput.contains("今天")) {
            sb.append("今天=").append(today).append("; ");
        }
        if (userInput.contains("明天")) {
            sb.append("明天=").append(today.plusDays(1)).append("; ");
        }
        if (userInput.contains("后天")) {
            sb.append("后天=").append(today.plusDays(2)).append("; ");
        }
        if (userInput.contains("下周六")) {
            sb.append("下周六=").append(getNextSaturday()).append("; ");
        }
        if (userInput.contains("下周日") || userInput.contains("下周天")) {
            sb.append("下周日=").append(getNextSunday()).append("; ");
        }
        if (userInput.contains("下周")) {
            sb.append("下周范围=").append(today.plusWeeks(1)).append("至").append(today.plusWeeks(2)).append("; ");
        }
        if (userInput.contains("本周")) {
            sb.append("本周范围=").append(today).append("至").append(today.plusWeeks(1)).append("; ");
        }

        return sb.length() > 0 ? sb.toString() : "无特殊时间表达";
    }

    /**
     * 基于规则解析时间参数并填充到 params Map 中（start_date, start_date_after, start_date_before）。
     */
    public static void resolveTimeParams(String userInput, Map<String, Object> params) {
        LocalDate today = LocalDate.now();

        if (userInput.contains("今天")) {
            params.put("start_date", today.toString());
        } else if (userInput.contains("明天")) {
            params.put("start_date", today.plusDays(1).toString());
        } else if (userInput.contains("后天")) {
            params.put("start_date", today.plusDays(2).toString());
        } else if (userInput.contains("下周六")) {
            params.put("start_date", getNextSaturday().toString());
        } else if (userInput.contains("下周日") || userInput.contains("下周天")) {
            params.put("start_date", getNextSunday().toString());
        } else if (userInput.contains("下周")) {
            params.put("start_date_after", today.plusWeeks(1).toString());
            params.put("start_date_before", today.plusWeeks(2).toString());
        } else if (userInput.contains("本周")) {
            params.put("start_date_after", today.toString());
            params.put("start_date_before", today.plusWeeks(1).toString());
        } else if (userInput.contains("周末")) {
            LocalDate nextSat = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            LocalDate nextSun = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            params.put("start_date_after", nextSat.toString());
            params.put("start_date_before", nextSun.plusDays(1).toString());
        }
    }

    public static LocalDate getNextSaturday() {
        LocalDate today = LocalDate.now();
        LocalDate nextSat = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nextSat = nextSat.plusWeeks(1);
        }
        return nextSat;
    }

    public static LocalDate getNextSunday() {
        LocalDate today = LocalDate.now();
        LocalDate nextSun = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nextSun = nextSun.plusWeeks(1);
        }
        return nextSun;
    }
}
