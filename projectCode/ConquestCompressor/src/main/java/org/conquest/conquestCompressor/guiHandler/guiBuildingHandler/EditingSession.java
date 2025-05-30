package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler;

import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;
import org.conquest.conquestCompressor.functionalHandler.recipeHandler.RecipeModel;

import java.util.UUID;

/**
 * ✏️ EditingSession
 * Tracks player session context during recipe or compressor GUI edits.
 */
public class EditingSession {

    private final UUID playerId;
    private long lastEditTime;

    private Runnable confirmAction;
    private Runnable cancelAction;
    private String confirmContextKey;

    private RecipeModel editingRecipe;
    private CompressorModel editingCompressor;

    private boolean closed = false;

    public EditingSession(UUID playerId) {
        this.playerId = playerId;
        this.lastEditTime = System.currentTimeMillis();
    }

    // ────────────── Core ──────────────

    public UUID getPlayerId() {
        return playerId;
    }

    public long getLastEditTime() {
        return lastEditTime;
    }

    public void touch() {
        this.lastEditTime = System.currentTimeMillis();
    }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - lastEditTime > timeoutMillis;
    }

    public void markClosed() {
        this.closed = true;
    }

    public void markOpen() {
        this.closed = false;
    }

    public boolean wasClosed() {
        return closed;
    }

    // ────────────── Confirm Context ──────────────

    public void setConfirmContext(String key, Runnable confirmAction, Runnable cancelAction) {
        this.confirmContextKey = key;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    public void clearConfirmContext() {
        this.confirmContextKey = null;
        this.confirmAction = null;
        this.cancelAction = null;
    }

    public boolean hasConfirmContext() {
        return confirmContextKey != null && confirmAction != null;
    }

    public String getConfirmContextKey() {
        return confirmContextKey;
    }

    public Runnable getConfirmAction() {
        return confirmAction;
    }

    public Runnable getCancelAction() {
        return cancelAction;
    }

    // ────────────── Recipe Context ──────────────

    public RecipeModel getEditingRecipe() {
        return editingRecipe;
    }

    public void setEditingRecipe(RecipeModel editingRecipe) {
        this.editingRecipe = editingRecipe;
    }

    public boolean isEditingRecipe() {
        return editingRecipe != null;
    }

    public void clearEditingRecipe() {
        this.editingRecipe = null;
    }

    // ────────────── Compressor Context ──────────────

    public CompressorModel getEditingCompressor() {
        return editingCompressor;
    }

    public void setEditingCompressor(CompressorModel editingCompressor) {
        this.editingCompressor = editingCompressor;
    }

    public boolean isEditingCompressor() {
        return editingCompressor != null;
    }

    public void clearEditingCompressor() {
        this.editingCompressor = null;
    }

    // ────────────── Global Clear ──────────────

    public void clearAllEditingContext() {
        clearConfirmContext();
        clearEditingRecipe();
        clearEditingCompressor();
    }
}
