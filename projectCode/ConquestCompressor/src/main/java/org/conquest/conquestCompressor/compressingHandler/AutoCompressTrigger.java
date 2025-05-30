package org.conquest.conquestCompressor.compressingHandler;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🕒 AutoCompressTrigger
 * Represents either an interval-based or event-based auto compression trigger.
 */
public class AutoCompressTrigger {

    public enum Type {
        INTERVAL,
        EVENT
    }

    private final Type type;
    private final long intervalMillis; // Only used if type == INTERVAL
    private final String eventKey;     // Only used if type == EVENT

    // 🧠 Add all supported event-based triggers here
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "ON_ITEM_PICKUP",
            "ON_CONTAINER_INVENTORY_OPEN",
            "ON_CONTAINER_INVENTORY_CLOSE",
            "ON_SHIFT_LEFT_CLICK",
            "ON_SHIFT_RIGHT_CLICK",
            "ON_LEFT_CLICK",
            "ON_RIGHT_CLICK"
    );

    private AutoCompressTrigger(Type type, long intervalMillis, String eventKey) {
        this.type = type;
        this.intervalMillis = intervalMillis;
        this.eventKey = eventKey;
    }

    public static AutoCompressTrigger fromConfig(String raw) {
        if (raw == null) return defaultTrigger();

        raw = raw.trim().toUpperCase(Locale.ROOT);

        // 🧱 Normalize legacy config aliases
        if (raw.equals("ON_PLAYER_INVENTORY_OPEN")) raw = "ON_CONTAINER_INVENTORY_OPEN";
        if (raw.equals("ON_PLAYER_INVENTORY_CLOSE")) raw = "ON_CONTAINER_INVENTORY_CLOSE";

        // ⚡ Check for event-style triggers
        if (SUPPORTED_EVENTS.contains(raw)) {
            return new AutoCompressTrigger(Type.EVENT, -1, raw);
        }

        // ⏱️ Check for interval-based syntax (e.g., 5s, 2m, 500ms)
        Pattern pattern = Pattern.compile("^(\\d+)(MS|S|M)$");
        Matcher matcher = pattern.matcher(raw);
        if (matcher.matches()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            long millis = switch (unit) {
                case "MS" -> value;
                case "S" -> value * 1000L;
                case "M" -> value * 60_000L;
                default -> -1;
            };

            if (millis > 0) {
                return new AutoCompressTrigger(Type.INTERVAL, millis, null);
            }
        }

        return defaultTrigger();
    }

    private static AutoCompressTrigger defaultTrigger() {
        return new AutoCompressTrigger(Type.INTERVAL, 60_000L, null); // ⏳ Default: every 1 minute
    }

    public boolean isInterval() {
        return type == Type.INTERVAL;
    }

    public boolean isEvent() {
        return type == Type.EVENT;
    }

    public long getIntervalMillis() {
        return intervalMillis;
    }

    public String getEventKey() {
        return eventKey;
    }

    public Type getType() {
        return type;
    }
}
