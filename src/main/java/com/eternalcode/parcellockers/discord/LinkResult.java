package com.eternalcode.parcellockers.discord;

public enum LinkResult {
    SUCCESS,
    PLAYER_ALREADY_LINKED,
    DISCORD_ALREADY_LINKED,
    DISCORD_USER_NOT_FOUND,
    VERIFICATION_PENDING,
    CANNOT_SEND_DM,
    GENERIC_FAILURE
}
