package com.eternalcode.parcellockers.configuration.composer;

import panda.std.Result;

import java.time.Duration;

public class DurationComposer implements SimpleComposer<Duration> {

    @Override
    public Result<Duration, Exception> deserialize(String input) {
        try {
            if (input == null || input.isBlank()) {
                return Result.error(new IllegalArgumentException("Input cannot be null or blank"));
            }

            // Remove spaces and convert to uppercase (e.g., "1h 30m 5s" -> "1H30M5S")
            String normalized = input.replace(" ", "").toUpperCase();

            // Re-add the "PT" prefix to make it ISO 8601 compliant
            Duration duration = Duration.parse("PT" + normalized);

            return Result.ok(duration);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @Override
    public Result<String, Exception> serialize(Duration duration) {
        return Result.ok(duration.toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase());
    }
}
