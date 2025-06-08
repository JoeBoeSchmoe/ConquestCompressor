package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler;

import org.conquest.conquestCompressor.functionalHandler.compressorHandler.CompressorModel;

import java.util.UUID;

/**
 * ✏️ EditingSession
 * Tracks player session context during recipe or compressor GUI edits.
 */
public class EditingSession {

    public enum SessionMode {
        CREATING,
        EDITING
    }

    private final UUID playerId;
    private final SessionMode mode;
    private long lastEditTime;

    private Runnable confirmAction;
    private Runnable cancelAction;
    private String confirmContextKey;

    private CompressorModel editingCompressor;

    private boolean closed = false;

    public EditingSession(UUID playerId, SessionMode mode) {
        this.playerId = playerId;
        this.mode = mode;
        this.lastEditTime = System.currentTimeMillis();
    }

    // ────────────── Core ──────────────

    public UUID getPlayerId() {
        return playerId;
    }

    public SessionMode getMode() {
        return mode;
    }

    public boolean isCreatingMode() {
        return mode == SessionMode.CREATING;
    }

    public boolean isEditingMode() {
        return mode == SessionMode.EDITING;
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
        clearEditingCompressor();
    }
}
