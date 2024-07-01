package com.eternalcode.parcellockers.shared;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Disclaimer - Bukkit {@link org.bukkit.Location} storage may cause a memory leak because it is a wrapper for
 * coordinates and {@link org.bukkit.World} reference. If you need to store location, use {@link Position} and
 * {@link PositionAdapter}.
 */
public record Position(int x, int y, int z, String world) {

    public static final String NONE_WORLD = "__NONE__";

    private static final Pattern PARSE_FORMAT = Pattern.compile("Position\\{x=(?<x>-?\\d+), y=(?<y>-?\\d+), z=(?<z>-?\\d+), world='(?<world>.+)'}");

    public static Position parse(String parse) {
        Matcher matcher = PARSE_FORMAT.matcher(parse);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid position format: " + parse);
        }

        return new Position(
            Integer.parseInt(matcher.group("x")),
            Integer.parseInt(matcher.group("y")),
            Integer.parseInt(matcher.group("z")),
            matcher.group("world")
        );
    }

    /**
     * Returns the distance between two positions.
     *
     * @param a
     * @param b
     * @return the distance between two positions
     */
    
    public static int distance(Position a, Position b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y()) + Math.abs(a.z() - b.z());
    }

    /**
     * Returns whether the two positions are within the specified radius.
     *
     * @param a      the first position
     * @param b      the second position
     * @param radius the radius
     * @return whether the two positions are within the specified radius
     */

    public static boolean isWithin(Position a, Position b, int radius) {
        return distance(a, b) <= radius;
    }

    public boolean isNoneWorld() {
        return this.world.equals(NONE_WORLD);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Position position = (Position) o;

        return this.x == position.x
            && this.y == position.y
            && this.z == position.z
            && this.world.equals(position.world);
    }

    @Override
    public String toString() {
        return "Position{" +
            "x=" + this.x +
            ", y=" + this.y +
            ", z=" + this.z +
            ", world='" + this.world + '\'' +
            '}';
    }
}
