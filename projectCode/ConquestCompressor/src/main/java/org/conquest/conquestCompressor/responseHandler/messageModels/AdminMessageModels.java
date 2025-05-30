package org.conquest.conquestCompressor.responseHandler.messageModels;

/**
 * 👑 AdminMessageModels
 * Enum keys for referencing structured adminMessages.yml paths.
 * All keys correspond to entries in messages.adminMessages.yml.
 */
public enum AdminMessageModels {

    // ✅ Basic usage and fallback
    ADMIN_USAGE_HINT("admin-usage-hint"),
    UNKNOWN_ADMIN_COMMAND("unknown-admin-command"),

    // 🔄 Reload
    CONFIG_RELOADED("config-reloaded"),

    // 🆘 Help menu
    ADMIN_HELP("admin-help"),

    // ❌ Permissions
    NO_PERMISSION("no-permission"),

    // Compressor

    COMPRESSOR_NOT_FOUND("compressor-not-found"),
    EDIT_MISSING_KEY("edit-missing-key"),
    CREATE_MISSING_KEY("create-missing-key"),
    COMPRESSOR_DELETE_SUCCESS("compressor-delete-success"),
    COMPRESSOR_DELETE_FAIL("compressor-delete-fail"),
    COMPRESSOR_ALREADY_EXISTS("compressor-already-exists"),

    COMPRESSOR_USAGE_HINT("compressor-usage-hint"),

    // Recipe
    RECIPE_NOT_FOUND("recipe-not-found"),
    EDIT_RECIPE_MISSING_KEY("edit-recipe-missing-key"),
    CREATE_RECIPE_MISSING_KEY("create-recipe-missing-key"),
    RECIPE_DELETE_SUCCESS("recipe-delete-success"),
    RECIPE_DELETE_FAIL("recipe-delete-fail"),
    RECIPE_USAGE_HINT("recipe-usage-hint"),

    PLACEHOLDER_REQUIRES_ITEM("placeholder-requires-item"),
    GUI_INVALID_ITEMS("gui-invalid-items");

    private final String path;

    AdminMessageModels(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
