package org.Vikuro.mobbuckets.listener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class BucketUseLockout {

    private static final int LOCKOUT_TICKS = 3;
    private static final Map<UUID, Integer> LOCKOUTS = new HashMap<>();

    public static void lock(UUID uuid) {
        LOCKOUTS.put(uuid, LOCKOUT_TICKS);
    }

    public static boolean isLocked(UUID uuid) {
        return LOCKOUTS.containsKey(uuid);
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, Integer>> iterator = LOCKOUTS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue() - 1;

            if (ticksLeft <= 0) {
                iterator.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }
    }
}