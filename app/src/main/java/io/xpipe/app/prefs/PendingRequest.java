package io.xpipe.app.prefs;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a pending request to KeePassXC.
 * This class tracks the request details and provides methods to complete or cancel the request.
 */
public class PendingRequest {
    private final String requestId;
    private final String action;
    private final CompletableFuture<String> future;
    private final long timestamp;
    private final long timeout;
    private boolean completed;

    /**
     * Creates a new pending request.
     * 
     * @param requestId The unique ID of the request
     * @param action The action being performed (e.g., "associate", "get-logins")
     * @param future The CompletableFuture that will be completed when the response is received
     * @param timeout The timeout in milliseconds
     */
    public PendingRequest(String requestId, String action, CompletableFuture<String> future, long timeout) {
        this.requestId = requestId;
        this.action = action;
        this.future = future;
        this.timeout = timeout;
        this.timestamp = System.currentTimeMillis();
        this.completed = false;
    }

    /**
     * Gets the request ID.
     * 
     * @return The request ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Gets the action.
     * 
     * @return The action
     */
    public String getAction() {
        return action;
    }

    /**
     * Gets the completable future.
     * 
     * @return The future
     */
    public CompletableFuture<String> getFuture() {
        return future;
    }

    /**
     * Gets the timestamp when the request was created.
     * 
     * @return The timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the timeout duration.
     * 
     * @return The timeout in milliseconds
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Checks if the request is completed.
     * 
     * @return True if the request is completed, false otherwise
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Completes the request with the given response.
     * 
     * @param response The response from KeePassXC
     * @return True if the request was completed, false if it was already completed
     */
    public boolean complete(String response) {
        if (completed) {
            return false;
        }
        
        completed = true;
        future.complete(response);
        return true;
    }

    /**
     * Completes the request exceptionally with a timeout.
     * 
     * @return True if the request was completed, false if it was already completed
     */
    public boolean timeout() {
        if (completed) {
            return false;
        }
        
        completed = true;
        future.completeExceptionally(new TimeoutException("Request timed out after " + timeout + "ms"));
        return true;
    }

    /**
     * Cancels the request.
     * 
     * @param reason The reason for cancellation
     * @return True if the request was cancelled, false if it was already completed
     */
    public boolean cancel(String reason) {
        if (completed) {
            return false;
        }
        
        completed = true;
        future.completeExceptionally(new RuntimeException("Request cancelled: " + reason));
        return true;
    }

    /**
     * Checks if the request has timed out.
     * 
     * @return True if the request has timed out, false otherwise
     */
    public boolean isTimedOut() {
        return !completed && System.currentTimeMillis() - timestamp > timeout;
    }

    /**
     * Exception class for request timeouts.
     */
    public static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }
}