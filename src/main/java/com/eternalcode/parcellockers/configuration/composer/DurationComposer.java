package com.eternalcode.parcellockers.configuration.composer;

import panda.std.Result;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationComposer implements SimpleComposer<Duration> {

    // Regex to match each time component individually
    private static final Pattern TIME_PATTERN = Pattern.compile("(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?", Pattern.CASE_INSENSITIVE);

    @Override
    public Result<String, Exception> serialize(Duration duration) {
        if (duration == null) {
            return Result.error(new IllegalArgumentException("Duration cannot be null"));
        }

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append("d ");
        }
        if (hours > 0) {
            result.append(hours).append("h ");
        }
        if (minutes > 0) {
            result.append(minutes).append("m ");
        }
        if (seconds > 0 || result.isEmpty()) { // Always include seconds if no other components
            result.append(seconds).append("s");
        }

        return Result.ok(result.toString().trim());
    }

    @Override
    public Result<Duration, Exception> deserialize(String input) {
        try {
            if (input == null || input.isBlank()) {
                return Result.error(new IllegalArgumentException("Input cannot be null or blank"));
            }

            // If input is already in ISO-8601 format
            if (input.toUpperCase().startsWith("P")) {
                return Result.ok(Duration.parse(input));
            }

            // For simple case: just a number (assume seconds)
            if (input.matches("\\d+(\\.\\d+)?")) {
                if (input.contains(".")) {
                    double seconds = Double.parseDouble(input);
                    return Result.ok(Duration.ofMillis((long) (seconds * 1000)));
                } else {
                    return Result.ok(Duration.ofSeconds(Long.parseLong(input)));
                }
            }

            // Parse custom format (e.g., "1d 2h 30m 15s")
            Duration duration = Duration.ZERO;
            Matcher matcher = TIME_PATTERN.matcher(input);

            if (matcher.matches()) {
                String days = matcher.group(1);
                String hours = matcher.group(2);
                String minutes = matcher.group(3);
                String seconds = matcher.group(4);

                if (days != null) {
                    duration = duration.plusDays(Long.parseLong(days));
                }
                if (hours != null) {
                    duration = duration.plusHours(Long.parseLong(hours));
                }
                if (minutes != null) {
                    duration = duration.plusMinutes(Long.parseLong(minutes));
                }
                if (seconds != null) {
                    duration = duration.plusSeconds(Long.parseLong(seconds));
                }

                return Result.ok(duration);
            }

            return Result.error(new IllegalArgumentException(
                "Invalid duration format. Expected format like '1d 2h 3m 4s' or ISO-8601 duration."));
        } catch (Exception e) {
            return Result.error(new IllegalArgumentException("Invalid duration format. Expected format like '1d 2h 3m 4s' or ISO-8601 duration."));
        }
    }
}
