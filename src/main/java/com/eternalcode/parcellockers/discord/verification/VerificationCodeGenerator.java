package com.eternalcode.parcellockers.discord.verification;

import java.util.concurrent.ThreadLocalRandom;

class VerificationCodeGenerator {

    private static final int MIN_CODE = 1000;
    private static final int MAX_CODE = 10000;

    String generate() {
        int code = ThreadLocalRandom.current().nextInt(MIN_CODE, MAX_CODE);
        return String.valueOf(code);
    }
}
