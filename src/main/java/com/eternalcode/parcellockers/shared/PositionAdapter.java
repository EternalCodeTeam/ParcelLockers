package com.eternalcode.parcellockers.shared;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class PositionAdapter {

    private PositionAdapter() {}

    public static Position convert(Location location) {
        if (location.getWorld() == null) {
            throw new IllegalStateException("World is not defined");
        }

        return new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
    }

    public static Location convert(Position position) {
        World world = Bukkit.getWorld(position.world());

        if (world == null) {
            throw new IllegalStateException("World is not defined");
        }

        return new Location(world, position.x(), position.y(), position.z());
    }

}
