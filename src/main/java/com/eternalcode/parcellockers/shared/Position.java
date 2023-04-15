package com.eternalcode.parcellockers.shared;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Disclaimer - Bukkit {@link org.bukkit.Location} storage may cause a memory leak, because it is a wrapper for
 * coordinates and {@link org.bukkit.World} reference. If you need to store location use {@link Position} and
 * {@link PositionAdapter}.
 */
public record Position(double x, double y, double z, float yaw, float pitch, String world) {

    public final static String NONE_WORLD = "__NONE__";

    private final static Pattern PARSE_FORMAT = Pattern.compile("Position\\{x=(?<x>-?[\\d.]+), y=(?<y>-?[\\d.]+), z=(?<z>-?[\\d.]+), yaw=(?<yaw>-?[\\d.]+), pitch=(?<pitch>-?[\\d.]+), world='(?<world>.+)'}");

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

        return Double.compare(position.x, this.x) == 0
                && Double.compare(position.y, this.y) == 0
                && Double.compare(position.z, this.z) == 0
                && Float.compare(position.yaw, this.yaw) == 0
                && Float.compare(position.pitch, this.pitch) == 0
                && this.world.equals(position.world);
    }

    @Override
    public String toString() {
        return "Position{" +
                "x=" + this.x +
                ", y=" + this.y +
                ", z=" + this.z +
                ", yaw=" + this.yaw +
                ", pitch=" + this.pitch +
                ", world='" + this.world + '\'' +
                '}';
    }

    public static Position parse(String parse) {
        Matcher matcher = PARSE_FORMAT.matcher(parse);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid position format: " + parse);
        }

        return new Position(
                Double.parseDouble(matcher.group("x")),
                Double.parseDouble(matcher.group("y")),
                Double.parseDouble(matcher.group("z")),
                Float.parseFloat(matcher.group("yaw")),
                Float.parseFloat(matcher.group("pitch")),
                matcher.group("world")
        );
    }
}
