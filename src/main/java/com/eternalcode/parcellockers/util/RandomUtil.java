package com.eternalcode.parcellockers.util;

import org.jetbrains.annotations.TestOnly;

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

    @TestOnly
    public static <T> T randomEnum(Class<T> clazz){
        int x = RANDOM.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    @TestOnly
    public static String randomParcelDescription() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 16;

       return RANDOM.ints(leftLimit, rightLimit + 1)
            .limit(targetStringLength)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }

}
