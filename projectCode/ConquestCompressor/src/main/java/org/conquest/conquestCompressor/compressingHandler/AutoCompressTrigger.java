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

    // ---------------------------------------------------------------------
    // ✅ Legacy factory — preserved as-is for backward compatibility
    // ---------------------------------------------------------------------
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
        Long ms = parseDurationMillis(normalized);
        if (ms != null && ms > 0) {
            return new AutoCompressTrigger(Type.INTERVAL, ms, null);
        }

        // Fallback
        return defaultTrigger();
    }

    // ---------------------------------------------------------------------
    // ✅ New factories — for modern config (AUTO vs MANUAL)
    // ---------------------------------------------------------------------

    /**
     * Parses the modern strategy + interval (with legacy fallback).
     * If strategy == MANUAL → returns a "disabled" INTERVAL (0ms) so callers can skip scheduling.
     * If strategy == AUTO   → uses nestedInterval if present, else legacyFallback.
     */
    public static AutoCompressTrigger fromStrategy(String strategy,
                                                   String nestedInterval,
                                                   String legacyFallback) {
        String s = (strategy == null ? "AUTO" : strategy.trim().toUpperCase(Locale.ROOT));
        if ("MANUAL".equals(s)) {
            // Manual mode disables auto scheduling.
            return disabled();
        }

        // AUTO (or anything else non-MANUAL): prefer nested, fallback to legacy
        String candidate = (nestedInterval != null && !nestedInterval.isBlank())
                ? nestedInterval
                : legacyFallback;

        if (candidate == null || candidate.isBlank()) {
            return defaultTrigger(); // keep prior default behavior
        }

        Long ms = parseDurationMillis(candidate);
        if (ms != null) {
            // If parsed 0 or negative, treat as disabled
            if (ms <= 0L) return disabled();
            return new AutoCompressTrigger(Type.INTERVAL, ms, null);
        }

        // Could be an old event token accidentally routed here — preserve old behavior
        String upper = candidate.trim().toUpperCase(Locale.ROOT);
        if (ALIASES.containsKey(upper)) upper = ALIASES.get(upper);
        if (SUPPORTED_EVENTS.contains(upper)) {
            EventKey key = EventKey.valueOf(upper);
            return new AutoCompressTrigger(Type.EVENT, -1L, key);
        }

        return defaultTrigger();
    }

    /**
     * Convenience overload when you only have a single interval source (modern nested key).
     */
    public static AutoCompressTrigger fromStrategy(String strategy, String nestedInterval) {
        return fromStrategy(strategy, nestedInterval, null);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static Long parseDurationMillis(String raw) {
        if (raw == null) return null;
        Matcher matcher = DURATION.matcher(raw.trim());
        if (!matcher.matches()) return null;

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);

        return switch (unit) {
            case "ms" -> value;
            case "s"  -> value * 1000L;
            case "m"  -> value * 60_000L;
            default   -> null;
        };
    }

    private static AutoCompressTrigger defaultTrigger() {
        return new AutoCompressTrigger(Type.INTERVAL, 60_000L, null); // ⏳ Default: every 1 minute
    }

    /** Returns an INTERVAL trigger that indicates "disabled" (intervalMillis == 0). */
    private static AutoCompressTrigger disabled() {
        return new AutoCompressTrigger(Type.INTERVAL, 0L, null);
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public boolean isInterval() {
        return type == Type.INTERVAL;
    }

    public boolean isEvent() {
        return type == Type.EVENT;
    }

    /** Interval in milliseconds (only meaningful when isInterval() == true). */
    public long getIntervalMillis() {
        return intervalMillis;
    }

    /** True if this represents an intentionally disabled auto schedule (AUTO off because MANUAL is active). */
    public boolean isDisabled() {
        return isInterval() && eventKey == null && intervalMillis <= 0L;
    }

    public String getEventKey() {
        return eventKey == null ? null : eventKey.name();
    }

    public Type getType() {
        return type;
    }
}