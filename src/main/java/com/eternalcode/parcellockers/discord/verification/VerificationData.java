package com.eternalcode.parcellockers.discord.verification;

/**
 * Represents a pending Discord verification request.
 *
 * @param discordId the Discord user ID
 * @param code the verification code
 */
record VerificationData(String discordId, String code) {}
