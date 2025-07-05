package com.eternalcode.parcellockers.configuration.composer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import panda.std.Result;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationComposerTest {

    private final DurationComposer composer = new DurationComposer();

    @Test
    @DisplayName("Serialize valid ISO-8601 duration")
    void testSerializeValidDuration() {
        Duration duration = Duration.ofDays(1).plusHours(2).plusMinutes(30).plusSeconds(15);
        Result<String, Exception> result = composer.serialize(duration);

        assertTrue(result.isOk());
        assertEquals("1d 2h 30m 15s", result.get());
    }

    @Test
    @DisplayName("Serialize invalid (null) duration")
    void testSerializeNullDuration() {
        Result<String, Exception> result = composer.serialize(null);

        assertTrue(result.isErr());
        assertEquals("Duration cannot be null", result.getError().getMessage());
    }

    @Test
    @DisplayName("Deserialize valid ISO-8601 format")
    void testDeserializeValidIso8601Format() {
        String input = "PT1H30M";
        Result<Duration, Exception> result = composer.deserialize(input);

        assertTrue(result.isOk());
        assertEquals(Duration.ofHours(1).plusMinutes(30), result.get());
    }

    @Test
    @DisplayName("Deserialize valid custom format")
    void testDeserializeValidCustomFormat() {
        String input = "1d 2h 30m 15s";
        Result<Duration, Exception> result = composer.deserialize(input);

        assertTrue(result.isOk());
        assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(30).plusSeconds(15), result.get());
    }

    @Test
    @DisplayName("Deserialize valid simple number format")
    void testDeserializeSimpleNumber() {
        String input = "3600";
        Result<Duration, Exception> result = composer.deserialize(input);

        assertTrue(result.isOk());
        assertEquals(Duration.ofSeconds(3600), result.get());
    }

    @Test
    @DisplayName("Deserialize invalid format")
    void testDeserializeInvalidFormat() {
        String input = "invalid format";
        Result<Duration, Exception> result = composer.deserialize(input);

        assertTrue(result.isErr());
        assertEquals("Invalid duration format. Expected format like '1d 2h 3m 4s' or ISO-8601 duration.", result.getError().getMessage());
    }

    @Test
    @DisplayName("Deserialize empty input")
    void testDeserializeNullInput() {
        Result<Duration, Exception> result = composer.deserialize(null);

        assertTrue(result.isErr());
        assertEquals("Input cannot be null or blank", result.getError().getMessage());
    }

    @Test
    @DisplayName("Deserialize blank input")
    void testDeserializeBlankInput() {
        Result<Duration, Exception> result = composer.deserialize("   ");

        assertTrue(result.isErr());
        assertEquals("Input cannot be null or blank", result.getError().getMessage());
    }
}
