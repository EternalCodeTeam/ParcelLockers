package com.eternalcode.parcellockers.discord.verification;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates random verification codes for Discord account linking.
 */
class VerificationCodeGenerator {

    private static final int MIN_CODE = 1000;
    private static final int MAX_CODE = 10000;

    /**
     * Generates a random 4-digit verification code.
     *
     * @return a 4-digit verification code as a string
     */
    String generate() {
        int code = ThreadLocalRandom.current().nextInt(MIN_CODE, MAX_CODE);
        return String.valueOf(code);
    }
}
