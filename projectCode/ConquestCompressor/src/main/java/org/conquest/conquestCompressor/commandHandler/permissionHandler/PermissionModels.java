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

    // NEW: per-recipe wildcard (all recipes)
    USER_AUTO_RECIPE_ALL("conquestcompressor.user.auto.recipe.*"),

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

    // ─────────────────────────────────────────────
    // Per-recipe helpers
    // ─────────────────────────────────────────────

    /**
     * Builds the specific per-recipe permission node for the given recipe id.
     * Example: "iron_ingot" -> "conquestcompressor.user.auto.recipe.iron_ingot"
     */
    public static String userRecipeNode(String recipeId) {
        if (recipeId == null || recipeId.isEmpty()) return null;
        // If you want to enforce allowed chars, sanitize/validate here:
        // recipeId = recipeId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-\\.]", "");
        return "conquestcompressor.user.auto.recipe." + recipeId;
    }

    /**
     * True if the provided id looks like a safe permission suffix.
     * (Optional hardening if ids can be user-provided.)
     */
    public static boolean isValidRecipeId(String recipeId) {
        return recipeId != null && recipeId.matches("[A-Za-z0-9._\\-]+");
    }
}
