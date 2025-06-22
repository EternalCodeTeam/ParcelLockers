package com.eternalcode.parcellockers.configuration.composer;

import panda.std.Result;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationComposer implements SimpleComposer<Duration> {

    // Regex to match each time component individually
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(\\d+(?:\\.\\d+)?)\\s*([dhms])",
        Pattern.CASE_INSENSITIVE);

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
                    return Result.ok(Duration.ofSeconds((long) Double.parseDouble(input)));
                } else {
                    return Result.ok(Duration.ofSeconds(Long.parseLong(input)));
                }
            }

            if (Duration.parse(input).isNegative()) {
                return Result.error(new IllegalArgumentException("Negative durations are not allowed"));
            }

            // Parse custom format (e.g., "1d 2h 3m 4.5s")
            Duration duration = Duration.ZERO;
            Matcher matcher = TIME_PATTERN.matcher(input);

            boolean foundMatch = false;
            while (matcher.find()) {
                foundMatch = true;
                String value = matcher.group(1);
                String unit = matcher.group(2).toLowerCase();

                switch (unit) {
                    case "d":
                        duration = duration.plusDays(Long.parseLong(value));
                        break;
                    case "h":
                        duration = duration.plusHours(Long.parseLong(value));
                        break;
                    case "m":
                        duration = duration.plusMinutes(Long.parseLong(value));
                        break;
                    case "s":
                        if (value.contains(".")) {
                            double seconds = Double.parseDouble(value);
                            long wholeSeconds = (long) seconds;
                            long nanos = (long) ((seconds - wholeSeconds) * 1_000_000_000);
                            duration = duration.plusSeconds(wholeSeconds).plusNanos(nanos);
                        } else {
                            duration = duration.plusSeconds(Long.parseLong(value));
                        }
                        break;
                }
            }

            if (!foundMatch) {
                return Result.error(new IllegalArgumentException(
                    "Invalid duration format. Expected format like '1d 2h 3m 4s' or ISO-8601 duration."));
            }

            return Result.ok(duration);

        } catch (Exception e) {
            return Result.error(new IllegalArgumentException("Failed to parse duration: " + e.getMessage(), e));
        }
    }
}
