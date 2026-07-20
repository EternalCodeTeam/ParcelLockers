package com.eternalcode.parcellockers.util;

import com.eternalcode.commons.time.DurationParser;
import com.eternalcode.commons.time.TemporalAmountParser;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

public final class DurationUtil {

    private static final Duration ONE_SECOND = Duration.ofSeconds(1);
    private static final Pattern REFORMAT_PATTERN = Pattern.compile("(\\d+)([dhms]+)");
    private static final String REFORMAT_REPLACEMENT = "$1$2 ";

    private static final TemporalAmountParser<Duration> DURATION = new DurationParser()
        .withUnit("s", ChronoUnit.SECONDS)
        .withUnit("m", ChronoUnit.MINUTES)
        .withUnit("h", ChronoUnit.HOURS)
        .withUnit("d", ChronoUnit.DAYS);

    private DurationUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String format(Duration duration) {
        if (duration.compareTo(ONE_SECOND) < 0) {
            return "0s";
        }
        return reformat(DURATION.format(duration));
    }


    private static String reformat(String input) {
        return REFORMAT_PATTERN.matcher(input).replaceAll(REFORMAT_REPLACEMENT).trim();
    }
}
