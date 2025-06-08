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
    COMMAND_ON_COOLDOWN("command-on-cooldown"),

    // 🔁 Auto Compression Toggle
    TOGGLE_ON("toggle-on"),
    TOGGLE_OFF("toggle-off"),

    // 📘 Help Page
    USER_HELP("user-help");

    private final String path;

    UserMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
