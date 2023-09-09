package com.eternalcode.parcellockers.util;

import java.util.Random;

public class RandomUtil {

    private static final Random RANDOM = new Random();

    private RandomUtil() {
        throw new IllegalStateException("This is a utility class and cannot be instantiated");
    }

    public static String generateDeliveryCode() {
        int maxCode = 999999;
        int randomCode = RANDOM.nextInt(maxCode + 1);
        return String.format("%06d", randomCode);
    }

}
