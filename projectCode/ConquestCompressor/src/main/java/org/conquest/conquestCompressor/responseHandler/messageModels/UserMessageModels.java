package org.conquest.conquestCompressor.responseHandler.messageModels;

/**
 * 🎮 UserMessageModels
 * Enum keys for referencing structured userMessages.yml paths.
 */
public enum UserMessageModels {

    // ⛔ Not a player
    NOT_PLAYER("not-player"),

    // ❓ General command responses
    UNKNOWN_COMMAND("unknown-command"),
    USAGE_HINT("usage-hint"),

    // ⏱️ Cooldowns
    COMMAND_ON_COOLDOWN("command-on-cooldown");

    private final String path;

    UserMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
