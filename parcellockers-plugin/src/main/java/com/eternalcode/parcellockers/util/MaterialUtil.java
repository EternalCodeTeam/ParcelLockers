package com.eternalcode.parcellockers.util;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;

public final class MaterialUtil {

    private MaterialUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String format(Material material) {
        return StringUtils.capitalize(material.name().toLowerCase().replace("_", " "));
    }
}
