package org.Vikuro.mobbuckets.clutch;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DeathMessageManager {

    private static final List<String> PVP_CLUTCH_MESSAGES = List.of(
            "%victim% was used to clutch by %user%",
            "%victim% was sacrificed in a clutch by %user%",
            "%victim% broke %user%'s fall",
            "%victim% became %user%'s landing pad",
            "%victim% learned about gravity from %user%",
            "%victim% absorbed %user%'s impact",
            "%victim% volunteered as fall damage",
            "%victim% was promoted to airbag by %user%",
            "%victim% discovered why OSHA exists",
            "%victim% was converted into kinetic energy"
    );

    private static final List<String> PVE_FALL_MESSAGES = List.of(
            "%victim% fell from a high place while being used by %user%",
            "%victim% left the world due to a failed clutch",
            "%victim% was flattened by %user%'s fall",
            "%victim% forgot how gravity works",
            "%victim% cushioned %user%'s landing",
            "%victim% volunteered as fall damage",
            "%victim% became one with the ground"
    );

    private static final List<String> ENVIRONMENTAL_MESSAGES = List.of(
            "%victim% met an untimely end while bound to %user%",
            "%victim% suffered the consequences of association with %user%",
            "%victim% was collateral damage for %user%",
            "%victim% was in the wrong place at the wrong time",
            "%victim% should have picked a different user"
    );

    public static String randomPvpClutch(String victim, String user) {
        return format(random(PVP_CLUTCH_MESSAGES), victim, user);
    }

    public static String randomPveFall(String victim, String user) {
        return format(random(PVE_FALL_MESSAGES), victim, user);
    }

    public static String randomEnvironmental(String victim, String user) {
        return format(random(ENVIRONMENTAL_MESSAGES), victim, user);
    }

    private static String random(List<String> messages) {
        return messages.get(
                ThreadLocalRandom.current().nextInt(messages.size())
        );
    }

    private static String format(String message, String victim, String user) {
        return message
                .replace("%victim%", victim)
                .replace("%user%", user);
    }
}