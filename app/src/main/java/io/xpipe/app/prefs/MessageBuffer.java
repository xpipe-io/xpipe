package io.xpipe.app.prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages pending requests to KeePassXC.
 * This class tracks all pending requests and provides methods to add, complete, and cancel requests.
 */
public class MessageBuffer {
    private final Map<String, PendingRequest> requestsById;
    private final Map<String, List<PendingRequest>> requestsByAction;
    private final Object lock = new Object();

    /**
     * Creates a new message buffer.
     */
    public MessageBuffer() {
        this.requestsById = new ConcurrentHashMap<>();
        this.requestsByAction = new ConcurrentHashMap<>();
    }

    /**
     * Adds a new pending request to the buffer.
     * 
     * @param request The request to add
     */
    public void addRequest(PendingRequest request) {
        synchronized (lock) {
            requestsById.put(request.getRequestId(), request);
            
            requestsByAction.computeIfAbsent(request.getAction(), k -> new ArrayList<>())
                            .add(request);
        }
    }

    /**
     * Gets a request by its ID.
     * 
     * @param requestId The request ID
     * @return The request, or null if not found
     */
    public PendingRequest getRequestById(String requestId) {
        synchronized (lock) {
            return requestsById.get(requestId);
        }
    }

    /**
     * Gets all pending requests for a specific action.
     * 
     * @param action The action
     * @return A list of pending requests, or an empty list if none found
     */
    public List<PendingRequest> getRequestsByAction(String action) {
        synchronized (lock) {
            List<PendingRequest> requests = requestsByAction.get(action);
            if (requests == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(requests); // Return a copy to avoid concurrent modification
        }
    }

    /**
     * Completes a request with the given response.
     * 
     * @param requestId The request ID
     * @param response The response from KeePassXC
     * @return True if the request was completed, false if not found or already completed
     */
    public boolean completeRequest(String requestId, String response) {
        synchronized (lock) {
            PendingRequest request = requestsById.get(requestId);
            if (request == null) {
                return false;
            }
            
            boolean completed = request.complete(response);
            if (completed) {
                removeRequest(request);
            }
            return completed;
        }
    }

    /**
     * Completes all pending requests for a specific action.
     * This is useful for action-specific responses that don't include a request ID.
     * 
     * @param action The action
     * @param response The response from KeePassXC
     * @return The number of requests that were completed
     */
    public int completeRequestsByAction(String action, String response) {
        synchronized (lock) {
            List<PendingRequest> requests = requestsByAction.get(action);
            if (requests == null || requests.isEmpty()) {
                return 0;
            }
            
            int count = 0;
            List<PendingRequest> completedRequests = new ArrayList<>();
            
            for (PendingRequest request : requests) {
                if (request.complete(response)) {
                    completedRequests.add(request);
                    count++;
                }
            }
            
            // Remove completed requests
            for (PendingRequest request : completedRequests) {
                removeRequest(request);
            }
            
            return count;
        }
    }

    /**
     * Times out a request.
     * 
     * @param requestId The request ID
     * @return True if the request was timed out, false if not found or already completed
     */
    public boolean timeoutRequest(String requestId) {
        synchronized (lock) {
            PendingRequest request = requestsById.get(requestId);
            if (request == null) {
                return false;
            }
            
            boolean timedOut = request.timeout();
            if (timedOut) {
                removeRequest(request);
            }
            return timedOut;
        }
    }

    /**
     * Cancels a request.
     * 
     * @param requestId The request ID
     * @param reason The reason for cancellation
     * @return True if the request was cancelled, false if not found or already completed
     */
    public boolean cancelRequest(String requestId, String reason) {
        synchronized (lock) {
            PendingRequest request = requestsById.get(requestId);
            if (request == null) {
                return false;
            }
            
            boolean cancelled = request.cancel(reason);
            if (cancelled) {
                removeRequest(request);
            }
            return cancelled;
        }
    }

    /**
     * Removes a request from the buffer.
     * 
     * @param request The request to remove
     */
    private void removeRequest(PendingRequest request) {
        requestsById.remove(request.getRequestId());
        
        List<PendingRequest> actionRequests = requestsByAction.get(request.getAction());
        if (actionRequests != null) {
            actionRequests.remove(request);
            if (actionRequests.isEmpty()) {
                requestsByAction.remove(request.getAction());
            }
        }
    }

    /**
     * Cleans up timed-out requests.
     * 
     * @return The number of requests that were timed out
     */
    public int cleanupTimedOutRequests() {
        synchronized (lock) {
            List<PendingRequest> timedOutRequests = new ArrayList<>();
            
            for (PendingRequest request : requestsById.values()) {
                if (request.isTimedOut()) {
                    request.timeout();
                    timedOutRequests.add(request);
                }
            }
            
            // Remove timed-out requests
            for (PendingRequest request : timedOutRequests) {
                removeRequest(request);
            }
            
            return timedOutRequests.size();
        }
    }

    /**
     * Gets the number of pending requests.
     * 
     * @return The number of pending requests
     */
    public int getPendingRequestCount() {
        synchronized (lock) {
            return requestsById.size();
        }
    }
    
    /**
     * Extracts the request ID from a JSON response.
     * 
     * @param response The JSON response
     * @return The request ID, or null if not found
     */
    public static String extractRequestId(String response) {
        try {
            Pattern pattern = Pattern.compile("\"requestId\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            System.err.println("Error extracting requestId: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Extracts the action from a JSON response.
     * 
     * @param response The JSON response
     * @return The action, or null if not found
     */
    public static String extractAction(String response) {
        try {
            Pattern pattern = Pattern.compile("\"action\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            System.err.println("Error extracting action: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Extracts the nonce from a JSON response.
     * 
     * @param response The JSON response
     * @return The nonce, or null if not found
     */
    public static String extractNonce(String response) {
        try {
            Pattern pattern = Pattern.compile("\"nonce\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            System.err.println("Error extracting nonce: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Handles an incoming response from KeePassXC.
     * This method attempts to match the response to a pending request and complete it.
     * 
     * @param response The JSON response from KeePassXC
     * @return The number of requests that were completed
     */
    public int handleResponse(String response) {
        if (response == null || response.isEmpty()) {
            return 0;
        }
        
        synchronized (lock) {
            String requestId = extractRequestId(response);
            String action = extractAction(response);
            
            if (requestId != null) {
                // Try to complete by request ID first
                if (completeRequest(requestId, response)) {
                    return 1;
                }
            }
            
            if (action != null) {
                // Then try to complete by action
                return completeRequestsByAction(action, response);
            }
            
            return 0;
        }
    }
}