package org.conquest.conquestCompressor.commandHandler.permissionHandler;

/**
 * 🔐 PermissionModels
 * Enum of all permission nodes used in ConquestCompressor.
 */
public enum PermissionModels {

    // ─────────────────────────────────────────────
    // 🎮 User Permissions
    // ─────────────────────────────────────────────
    USER_HELP("conquestcompressor.user.help"),
    USER_AUTO("conquestcompressor.user.auto"),
    USER_TOGGLE("conquestcompressor.user.auto.toggle"),
    USER_ALL("conquestcompressor.user.*"),

    // ─────────────────────────────────────────────
    // 🛠 Admin Base & Wildcard
    // ─────────────────────────────────────────────
    ADMIN_BASE("conquestcompressor.admin"),
    ADMIN_ALL("conquestcompressor.admin.*"),

    // ─────────────────────────────────────────────
    // 🔁 Admin Subcommand Permissions
    // ─────────────────────────────────────────────
    ADMIN_RELOAD("conquestcompressor.admin.reload"),
    ADMIN_COMPRESSOR("conquestcompressor.admin.compressor"),
    ADMIN_RECIPE("conquestcompressor.admin.recipe");

    private final String node;

    PermissionModels(String node) {
        this.node = node;
    }

    /**
     * Returns the full permission string.
     */
    public String getNode() {
        return node;
    }

    @Override
    public String toString() {
        return node;
    }
}
