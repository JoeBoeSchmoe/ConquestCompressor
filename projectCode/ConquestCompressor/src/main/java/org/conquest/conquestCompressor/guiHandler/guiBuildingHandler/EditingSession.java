package org.conquest.conquestCompressor.guiHandler.guiBuildingHandler;

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

    private boolean closed = false;

    public EditingSession(UUID playerId) {
        this.playerId = playerId;
        this.lastEditTime = System.currentTimeMillis();
    }

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

    public void markClosed() {
        this.closed = true;
    }

    public void markOpen() {
        this.closed = false;
    }

    public boolean wasClosed() {
        return closed;
    }
}
