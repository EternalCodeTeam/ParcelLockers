package com.eternalcode.parcellockers.database;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LastLoginStorage {

    public static Map<UUID, Instant> lastLoginMap = new HashMap<>();
}
