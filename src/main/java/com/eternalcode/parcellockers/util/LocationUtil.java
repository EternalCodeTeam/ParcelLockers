package com.eternalcode.parcellockers.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {

    private LocationUtil() {
    }

    public static Location parseLocation(String locationString) {
        String[] parts = locationString.replace("Location{", "")
                .replace("}", "")
                .split(",");

        String worldName = parts[0].substring(parts[0].indexOf("=") + 1);
        double x = Double.parseDouble(parts[1].substring(parts[1].indexOf("=") + 1));
        double y = Double.parseDouble(parts[2].substring(parts[2].indexOf("=") + 1));
        double z = Double.parseDouble(parts[3].substring(parts[3].indexOf("=") + 1));
        float pitch = 0;
        float yaw = 0;
        if (!parts[4].isEmpty() && !parts[4].isBlank()) {
            pitch = Float.parseFloat(parts[4].substring(parts[4].indexOf("=") + 1));
        }
        if (!parts[5].isEmpty() && !parts[5].isBlank()) {
            yaw = Float.parseFloat(parts[5].substring(parts[5].indexOf("=") + 1));
        }

        World world = Bukkit.getWorld(worldName);
        return new Location(world, x, y, z, yaw, pitch);
    }

}
