package org.conquest.conquestCompressor.compressingHandler;

import java.util.Locale;
import java.util.Map;
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

    // Canonical event keys; add/remove as you add listeners
    public enum EventKey {
        ON_ITEM_PICKUP,
        ON_CONTAINER_INVENTORY_OPEN,
        ON_CONTAINER_INVENTORY_CLOSE,
        ON_SHIFT_LEFT_CLICK,
        ON_SHIFT_RIGHT_CLICK,
        ON_LEFT_CLICK,
        ON_RIGHT_CLICK
    }

    private final Type type;
    private final long intervalMillis;  // Only used if type == INTERVAL
    private final EventKey eventKey;    // Only used if type == EVENT

    // Supported events as a quick lookup set
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "ON_ITEM_PICKUP",
            "ON_CONTAINER_INVENTORY_OPEN",
            "ON_CONTAINER_INVENTORY_CLOSE",
            "ON_SHIFT_LEFT_CLICK",
            "ON_SHIFT_RIGHT_CLICK",
            "ON_LEFT_CLICK",
            "ON_RIGHT_CLICK"
    );

    // Normalize legacy aliases -> canonical names
    private static final Map<String, String> ALIASES = Map.of(
            "ON_PLAYER_INVENTORY_OPEN",  "ON_CONTAINER_INVENTORY_OPEN",
            "ON_PLAYER_INVENTORY_CLOSE", "ON_CONTAINER_INVENTORY_CLOSE"
    );

    // Accepts forms like "500ms", "5s", "2m" with optional spaces/case-insensitive
    private static final Pattern DURATION = Pattern.compile("^\\s*(\\d+)\\s*(ms|s|m)\\s*$", Pattern.CASE_INSENSITIVE);

    private AutoCompressTrigger(Type type, long intervalMillis, EventKey eventKey) {
        this.type = type;
        this.intervalMillis = intervalMillis;
        this.eventKey = eventKey;
    }

    public static AutoCompressTrigger fromConfig(String raw) {
        if (raw == null) return defaultTrigger();

        String normalized = raw.trim();
        // Apply alias first (case-insensitive)
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (ALIASES.containsKey(upper)) {
            upper = ALIASES.get(upper);
        }

        // ⚡ Event-style triggers
        if (SUPPORTED_EVENTS.contains(upper)) {
            EventKey key = EventKey.valueOf(upper);
            return new AutoCompressTrigger(Type.EVENT, -1L, key);
        }

        // ⏱️ Interval-based syntax (e.g., "5s", "2m", "500ms")
        Matcher matcher = DURATION.matcher(normalized);
        if (matcher.matches()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);

            long millis = switch (unit) {
                case "ms" -> value;
                case "s"  -> value * 1000L;
                case "m"  -> value * 60_000L;
                default   -> -1L;
            };

            if (millis > 0) {
                return new AutoCompressTrigger(Type.INTERVAL, millis, null);
            }
        }

        // Fallback
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
        return eventKey == null ? null : eventKey.name();
    }

    public Type getType() {
        return type;
    }
}
