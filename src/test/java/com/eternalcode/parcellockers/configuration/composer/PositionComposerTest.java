package com.eternalcode.parcellockers.configuration.composer;

import com.eternalcode.parcellockers.shared.Position;
import org.junit.jupiter.api.Test;
import panda.std.AttemptFailedException;
import panda.std.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionComposerTest {

    private final PositionComposer positionComposer = new PositionComposer();

    @Test
    void testSerialize() {
        // Given
        Position position = new Position(10, 20, 30, "world");
        String expected = "Position{x=10, y=20, z=30, world='world'}";

        // When
        Result<String, Exception> result = this.positionComposer.serialize(position);

        // Then
        assertTrue(result.isOk());
        assertEquals(expected, result.get());
    }

    @Test
    void testDeserialize() {
        // Given
        String positionString = "Position{x=-50, y=64, z=120, world='world_nether'}";
        Position expected = new Position(-50, 64, 120, "world_nether");

        // When
        Result<Position, Exception> result = this.positionComposer.deserialize(positionString);

        // Then
        assertTrue(result.isOk());
        assertEquals(expected, result.get());
    }

    @Test
    void testDeserializeInvalidFormat() {
        // Given
        String invalidString = "world,-50,64,120";

        // When
        Result<Position, Exception> result = this.positionComposer.deserialize(invalidString);

        // Then
        assertTrue(result.isErr());
        assertInstanceOf(IllegalArgumentException.class, result.getError());
    }

    @Test
    void testSerializeNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> this.positionComposer.serialize(null));
    }

    @Test
    void testDeserializeNull() {
        // When & Then
        assertThrows(AttemptFailedException.class, () -> this.positionComposer.deserialize(null));
    }
}
