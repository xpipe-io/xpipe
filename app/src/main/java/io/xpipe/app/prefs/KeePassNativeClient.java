package io.xpipe.app.prefs;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ThreadHelper;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for communicating with KeePassXC using the native messaging protocol.
 * This implementation communicates with the actual running KeePassXC-proxy process
 * via stdin and stdout.
 *
 * Native messaging uses length-prefixed JSON messages over stdin/stdout.
 */
public class KeePassNativeClient {

    // Default timeouts for different operations (milliseconds)
    private static final long TIMEOUT_ASSOCIATE = 30000;      // Associate needs user interaction
    private static final long TIMEOUT_GET_LOGINS = 5000;      // Getting logins is usually fast
    private static final long TIMEOUT_TEST_ASSOCIATE = 2000;  // Testing association is quick
    private static final long TIMEOUT_GET_DATABASE_GROUPS = 3000; // Getting database groups

    private final Path proxyExecutable;
    private Process process;
    private String clientId;
    private TweetNaClHelper.KeyPair keyPair;
    private byte[] serverPublicKey;
    private boolean connected = false;
    private boolean associated = false;
    private Thread responseHandler;
    @Getter
    private KeePassAssociationKey associationKey;
    
    // Message buffer for handling requests/responses
    private final MessageBuffer messageBuffer = new MessageBuffer();
    private final Object responseNotifier = new Object();
    
    // Flag to indicate if key exchange is in progress
    private volatile boolean keyExchangeInProgress = false;

    public KeePassNativeClient(Path proxyExecutable) {this.proxyExecutable = proxyExecutable;}

    public void useExistingAssociationKey(KeePassAssociationKey key) {
        this.associationKey = key;
    }

    /**
     * Connects to KeePassXC via the provided input and output streams.
     * In a real application, these would be the streams connecting to KeePassXC.
     *
     * @return True if connection was successful, false otherwise
     * @throws IOException If there's an error connecting to KeePassXC
     */
    public void connect() throws IOException {
        // Generate a random client ID (24 bytes base64 encoded)
        this.clientId = TweetNaClHelper.encodeBase64(TweetNaClHelper.randomBytes(TweetNaClHelper.NONCE_SIZE));

        // Generate actual cryptographic keys
        this.keyPair = TweetNaClHelper.generateKeyPair();

        var pb = new ProcessBuilder(List.of(proxyExecutable.toString()));
        this.process = pb.start();
        
        // Start a thread to handle responses
        responseHandler = new Thread(this::handleResponses);
        responseHandler.setDaemon(true);
        responseHandler.start();
        
        connected = true;
    }
    
    /**
     * Performs a key exchange with KeePassXC.
     *
     * @return True if the key exchange was successful, false otherwise
     * @throws IOException If there's an error communicating with KeePassXC
     */
    public void exchangeKeys() throws IOException {
            // Generate a nonce
            byte[] nonceBytes = TweetNaClHelper.randomBytes(TweetNaClHelper.NONCE_SIZE);
            String nonce = TweetNaClHelper.encodeBase64(nonceBytes);
            
            // Convert our public key to base64
            String publicKeyB64 = TweetNaClHelper.encodeBase64(keyPair.getPublicKey());
            
            // Build the key exchange message - NOTE: This is NOT encrypted
            String requestId = UUID.randomUUID().toString();
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("action", "change-public-keys");
            messageMap.put("publicKey", publicKeyB64);
            messageMap.put("nonce", nonce);
            messageMap.put("clientID", clientId);
            messageMap.put("requestId", requestId);
            
            // Convert to JSON string
            String keyExchangeMessage = mapToJson(messageMap);
            
            // Send the message directly
            long startTime = System.currentTimeMillis();
            sendNativeMessage(keyExchangeMessage);
            
            // Wait for a direct response rather than using CompletableFuture
            // This is a special case because we can't use the encryption yet
            long timeout = 3000; // 3 seconds for key exchange
            
            while (System.currentTimeMillis() - startTime < timeout) {
                if (process.getInputStream().available() > 0) {
                    String response = receiveNativeMessage();
                    if (response.contains("change-public-keys")) {
                        // Use regex to extract the public key to avoid any JSON parsing issues
                        Pattern pattern = Pattern.compile("\"publicKey\":\"([^\"]+)\"");
                        Matcher matcher = pattern.matcher(response);

                        if (matcher.find()) {
                            String serverPubKeyB64 = matcher.group(1);

                            // Store the server's public key
                            this.serverPublicKey = TweetNaClHelper.decodeBase64(serverPubKeyB64);

                            // Check for success in the response
                            boolean success = response.contains("\"success\":\"true\"");
                            if (!success) {
                                throw new IllegalStateException("Key exchange failed");
                            } else {
                                return;
                            }
                        }
                    }
                }
                
                // Small sleep to prevent CPU hogging
                ThreadHelper.sleep(50);
            }

            throw new IllegalStateException("Key exchanged timed out");
    }
    
    /**
     * Tests the association with KeePassXC.
     *
     * @return True if associated, false otherwise
     * @throws IOException If there's an error communicating with KeePassXC
     */
    public void testAssociation() throws IOException {
        if (associationKey == null) {
            // We need to do an association first
            throw ErrorEvent.expected(new IllegalStateException("KeePassXC association failed or was cancelled"));
        }
        
        // Generate a nonce
        String nonce = TweetNaClHelper.encodeBase64(TweetNaClHelper.randomBytes(TweetNaClHelper.NONCE_SIZE));
        
        // Create the unencrypted message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("action", "test-associate");
        messageData.put("id", associationKey.getId());
        messageData.put("key", associationKey.getKey());
        
        // Encrypt the message
        String encryptedMessage = encrypt(messageData, nonce);
        if (encryptedMessage == null) {
            throw new IllegalStateException("Failed to encrypt test-associate message");
        }
        
        // Build the request
        Map<String, Object> request = new HashMap<>();
        request.put("action", "test-associate");
        request.put("message", encryptedMessage);
        request.put("nonce", nonce);
        request.put("clientID", clientId);
        
        String requestJson = mapToJson(request);
        
        // Send the request
        String responseJson = sendRequest("test-associate", requestJson, TIMEOUT_TEST_ASSOCIATE);
        if (responseJson == null) {
            throw new IllegalStateException("No response received from associated instance");
        }

        // Parse and decrypt the response
        Map<String, Object> responseMap = jsonToMap(responseJson);
        if (responseMap.containsKey("message") && responseMap.containsKey("nonce")) {
            String encryptedResponse = (String) responseMap.get("message");
            String responseNonce = (String) responseMap.get("nonce");

            String decryptedResponse = decrypt(encryptedResponse, responseNonce);
            if (decryptedResponse != null) {
                Map<String, Object> parsedResponse = jsonToMap(decryptedResponse);
                boolean success = parsedResponse.containsKey("success") &&
                                "true".equals(parsedResponse.get("success").toString());

                if (success) {
                    associated = true;
                } else {
                    throw new IllegalStateException("KeePassXC association failed");
                }
            }
        }
    }
    
    /**
     * Retrieves credentials from KeePassXC.
     *
     * @param url The URL to get credentials for
     * @return The response JSON, or null if failed
     * @throws IOException If there's an error communicating with KeePassXC
     */
    public String getLogins(String url) throws IOException {
        // Generate a nonce
        String nonce = TweetNaClHelper.encodeBase64(TweetNaClHelper.randomBytes(TweetNaClHelper.NONCE_SIZE));
        
        // Create the unencrypted message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("action", "get-logins");
        messageData.put("url", url);
        
        // Add the keys
        Map<String, Object> keyData = new HashMap<>();
        keyData.put("id", associationKey.getId());
        keyData.put("key", associationKey.getKey());
        
        messageData.put("keys", new Map[] { keyData });
        
        // Encrypt the message
        String encryptedMessage = encrypt(messageData, nonce);
        if (encryptedMessage == null) {
            return null;
        }
        
        // Build the request
        Map<String, Object> request = new HashMap<>();
        request.put("action", "get-logins");
        request.put("message", encryptedMessage);
        request.put("nonce", nonce);
        request.put("clientID", clientId);
        
        String requestJson = mapToJson(request);
        System.out.println("Sending get-logins message: " + requestJson);
        
        // Send the request
        String responseJson = sendRequest("get-logins", requestJson, TIMEOUT_GET_LOGINS);
        if (responseJson == null) {
            return null;
        }
        
        // Parse and decrypt the response
        try {
            Map<String, Object> responseMap = jsonToMap(responseJson);
            if (responseMap.containsKey("message") && responseMap.containsKey("nonce")) {
                String encryptedResponse = (String) responseMap.get("message");
                String responseNonce = (String) responseMap.get("nonce");
                
                return decrypt(encryptedResponse, responseNonce);
            }
        } catch (Exception e) {
            System.err.println("Error processing get-logins response: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Disconnects from KeePassXC.
     */
    public void disconnect() {
        if (responseHandler != null) {
            responseHandler.interrupt();
            responseHandler = null;
        }
        
        process.destroy();
        process = null;
    }
    
    /**
     * Sends a request to KeePassXC and waits for a response.
     *
     * @param action The action being performed (e.g., "associate", "get-logins")
     * @param message The JSON message to send
     * @param timeout The timeout in milliseconds
     * @return The response JSON, or null if timed out
     * @throws IOException If there's an error communicating with KeePassXC
     */
    private String sendRequest(String action, String message, long timeout) throws IOException {
        String requestId = extractRequestId(message);
        if (requestId == null) {
            // If no requestId in the message, generate one for tracking
            requestId = UUID.randomUUID().toString();
        }
        
        // Create a completable future for this request
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        // Create a pending request and add it to the message buffer
        PendingRequest request = new PendingRequest(requestId, action, responseFuture, timeout);
        messageBuffer.addRequest(request);
        
        // Send the message
        sendNativeMessage(message);
        
        // Notify the response handler that we've sent a message
        synchronized (responseNotifier) {
            responseNotifier.notify();
        }
        
        try {
            // Wait for the response with the specified timeout
            return responseFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Request interrupted: " + e.getMessage());
            return null;
        } catch (ExecutionException e) {
            System.err.println("Error in request execution: " + e.getMessage());
            return null;
        } catch (TimeoutException e) {
            System.err.println("Request timed out after " + timeout + "ms: " + action);
            return null;
        } finally {
            // Clean up timed-out requests
            messageBuffer.cleanupTimedOutRequests();
        }
    }

    /**
     * Extracts the requestId from a JSON message.
     *
     * @param message The JSON message
     * @return The requestId, or null if not found
     */
    private String extractRequestId(String message) {
        return MessageBuffer.extractRequestId(message);
    }
    
    /**
     * Continuously reads and processes responses from KeePassXC.
     */
    private void handleResponses() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // If key exchange is in progress, skip normal message handling
                if (keyExchangeInProgress) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                
                // Check if there's anything to read
                boolean hasData = false;
                try {
                    hasData = process.getInputStream().available() > 0;
                } catch (IOException e) {
                    System.err.println("Error checking input stream: " + e.getMessage());
                    continue;
                }
                
                if (hasData) {
                    try {
                        String response = receiveNativeMessage();
                        if (response != null) {
                            processResponse(response);
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading response: " + e.getMessage());
                    }
                } else {
                    // If nothing to read, wait efficiently
                    try {
                        synchronized (responseNotifier) {
                            responseNotifier.wait(100); // Wait up to 100ms for notification
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                // Periodically check for timed-out requests
                messageBuffer.cleanupTimedOutRequests();
            }
        } catch (Exception e) {
            System.err.println("Error in response handler: " + e.getMessage());
        }
    }
    
    /**
     * Process a response from KeePassXC.
     *
     * @param response The JSON response
     */
    private void processResponse(String response) {
        System.out.println("Received response: " + response);
        
        try {
            // Extract action
            String action = MessageBuffer.extractAction(response);
            
            // Special handling for action-specific responses
            if ("database-locked".equals(action) || "database-unlocked".equals(action)) {
                System.out.println("Database state changed: " + action);
                // Update state based on the action
                if ("database-locked".equals(action)) {
                    associated = false;
                }
                
                // Notify any waiting requests
                messageBuffer.handleResponse(response);
                return;
            }
            
            // Standard response handling - use the message buffer to complete the appropriate request
            int completedCount = messageBuffer.handleResponse(response);
            if (completedCount == 0) {
                System.out.println("Warning: Response did not match any pending request: " + response);
            }
        } catch (Exception e) {
            System.err.println("Error processing response: " + e.getMessage());
        }
    }
    
    /**
     * Sends a message to KeePassXC using the native messaging protocol.
     * The message is prefixed with a 32-bit length (little-endian).
     *
     * @param message The JSON message to send
     * @throws IOException If there's an error writing to the stream
     */
    private void sendNativeMessage(String message) throws IOException {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        int length = messageBytes.length;
        
        // Create a ByteBuffer with length in little-endian format
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
        lengthBuffer.putInt(length);

        process.getOutputStream().write(lengthBuffer.array());
        process.getOutputStream().write(messageBytes);
        process.getOutputStream().flush();
    }
    
    /**
     * Receives a message from KeePassXC using the native messaging protocol.
     * The message is prefixed with a 32-bit length (little-endian).
     *
     * @return The received JSON message as a string, or null if reading failed
     * @throws IOException If there's an error reading from the stream
     */
    private String receiveNativeMessage() throws IOException {
        // Read the length prefix (4 bytes, little-endian)
        byte[] lengthBytes = new byte[4];
        int bytesRead = process.getInputStream().read(lengthBytes);
        if (bytesRead != 4) {
            throw new IOException("Error reading received message");
        }
        
        // Convert bytes to integer (little-endian)
        ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
        lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int messageLength = lengthBuffer.getInt();

        // Read the actual message
        byte[] messageBytes = new byte[messageLength];
        var read = process.getInputStream().read(messageBytes);
        if (read != messageLength) {
            throw new IOException("Received message with " + read + " bytes but expected " + messageBytes.length);
        }
        
        return new String(messageBytes, StandardCharsets.UTF_8);
    }

    /**
     * Gets the database groups from KeePassXC.
     *
     * @return The JSON string containing the groups structure, or null if failed
     * @throws IOException If there's an error communicating with KeePassXC
     */
    public String getDatabaseGroups() throws IOException {
        if (!connected) {
            throw new IllegalStateException("Not connected to KeePassXC");
        }
        
        // Generate a nonce
        String nonce = TweetNaClHelper.encodeBase64(TweetNaClHelper.randomBytes(TweetNaClHelper.NONCE_SIZE));
        
        // Create the unencrypted message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("action", "get-database-groups");
        
        // Encrypt the message
        String encryptedMessage = encrypt(messageData, nonce);
        if (encryptedMessage == null) {
            System.err.println("Failed to encrypt get-database-groups message");
            return null;
        }
        
        // Build the request
        Map<String, Object> request = new HashMap<>();
        request.put("action", "get-database-groups");
        request.put("message", encryptedMessage);
        request.put("nonce", nonce);
        request.put("clientID", clientId);
        
        String requestJson = mapToJson(request);
        System.out.println("Sending get-database-groups message: " + requestJson);
        
        // Send the request
        String responseJson = sendRequest("get-database-groups", requestJson, TIMEOUT_GET_DATABASE_GROUPS);
        if (responseJson == null) {
            System.err.println("No response received from get-database-groups");
            return null;
        }
        
        // Parse and decrypt the response
        try {
            Map<String, Object> responseMap = jsonToMap(responseJson);
            if (responseMap.containsKey("message") && responseMap.containsKey("nonce")) {
                String encryptedResponse = (String) responseMap.get("message");
                String responseNonce = (String) responseMap.get("nonce");
                
                String decryptedResponse = decrypt(encryptedResponse, responseNonce);
                if (decryptedResponse != null) {
                    System.out.println("Received decrypted get-database-groups response: " + decryptedResponse);
                    return decryptedResponse;
                } else {
                    System.err.println("Failed to decrypt get-database-groups response");
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing get-database-groups response: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Encrypts a message for sending to KeePassXC.
     *
     * @param message The message to encrypt
     * @param nonce The nonce to use for encryption
     * @return The encrypted message, or null if encryption failed
     */
    private String encrypt(Map<String, Object> message, String nonce) {
        if (serverPublicKey == null) {
            System.err.println("Server public key not available for encryption");
            return null;
        }
        
        try {
            String messageJson = mapToJson(message);
            byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);
            byte[] nonceBytes = TweetNaClHelper.decodeBase64(nonce);
            
            byte[] encrypted = TweetNaClHelper.box(
                messageBytes,
                nonceBytes,
                serverPublicKey,
                keyPair.getSecretKey()
            );
            
            if (encrypted == null) {
                System.err.println("Encryption failed");
                return null;
            }
            
            return TweetNaClHelper.encodeBase64(encrypted);
        } catch (Exception e) {
            System.err.println("Error during encryption: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Decrypts a message received from KeePassXC.
     *
     * @param encryptedMessage The encrypted message
     * @param nonce The nonce used for encryption
     * @return The decrypted message, or null if decryption failed
     */
    private String decrypt(String encryptedMessage, String nonce) {
        if (serverPublicKey == null) {
            System.err.println("Server public key not available for decryption");
            return null;
        }
        
        try {
            byte[] messageBytes = TweetNaClHelper.decodeBase64(encryptedMessage);
            byte[] nonceBytes = TweetNaClHelper.decodeBase64(nonce);
            
            byte[] decrypted = TweetNaClHelper.boxOpen(
                messageBytes,
                nonceBytes,
                serverPublicKey,
                keyPair.getSecretKey()
            );
            
            if (decrypted == null) {
                System.err.println("Decryption failed");
                return null;
            }
            
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Error during decryption: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Associate with KeePassXC.
     *
     * @return True if successful, false otherwise
     * @throws IOException If there's an error communicating with KeePassXC
     */
    public boolean associate() throws IOException {
        // Generate a key pair for identification
        TweetNaClHelper.KeyPair idKeyPair = TweetNaClHelper.generateKeyPair();
        
        // Generate a nonce
        String nonce = TweetNaClHelper.encodeBase64(TweetNaClHelper.randomBytes(TweetNaClHelper.NONCE_SIZE));
        
        // Create the unencrypted message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("action", "associate");
        messageData.put("key", TweetNaClHelper.encodeBase64(keyPair.getPublicKey()));
        messageData.put("idKey", TweetNaClHelper.encodeBase64(idKeyPair.getPublicKey()));
        
        // Encrypt the message
        String encryptedMessage = encrypt(messageData, nonce);
        if (encryptedMessage == null) {
            return false;
        }
        
        // Build the request
        Map<String, Object> request = new HashMap<>();
        request.put("action", "associate");
        request.put("message", encryptedMessage);
        request.put("nonce", nonce);
        request.put("clientID", clientId);
        
        String requestJson = mapToJson(request);
        System.out.println("Sending associate message: " + requestJson);
        
        // Send the request using longer timeout as it requires user interaction
        String responseJson = sendRequest("associate", requestJson, TIMEOUT_ASSOCIATE);
        if (responseJson == null) {
            return false;
        }
        
        // Parse and decrypt the response
        try {
            Map<String, Object> responseMap = jsonToMap(responseJson);
            if (responseMap.containsKey("message") && responseMap.containsKey("nonce")) {
                String encryptedResponse = (String) responseMap.get("message");
                String responseNonce = (String) responseMap.get("nonce");
                
                String decryptedResponse = decrypt(encryptedResponse, responseNonce);
                if (decryptedResponse != null) {
                    Map<String, Object> parsedResponse = jsonToMap(decryptedResponse);
                    boolean success = parsedResponse.containsKey("success") &&
                                    "true".equals(parsedResponse.get("success").toString());
                    
                    if (success && parsedResponse.containsKey("id") && parsedResponse.containsKey("hash")) {
                        String id = (String) parsedResponse.get("id");
                        String hash = (String) parsedResponse.get("hash");

                        associationKey = new KeePassAssociationKey(id, TweetNaClHelper.encodeBase64(idKeyPair.getPublicKey()), hash);
                        associated = true;
                        
                        System.out.println("Association successful");
                        System.out.println("Database ID: " + id);
                        System.out.println("Database hash: " + hash);
                        
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing associate response: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Convert a map to a JSON string.
     */
    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJsonString((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map[]) {
                sb.append("[");
                Map[] maps = (Map[]) value;
                for (int i = 0; i < maps.length; i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(mapToJson(maps[i]));
                }
                sb.append("]");
            } else if (value == null) {
                sb.append("null");
            } else {
                sb.append("\"").append(escapeJsonString(value.toString())).append("\"");
            }
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Escape special characters in a JSON string.
     */
    private String escapeJsonString(String s) {
        if (s == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Convert a JSON string to a map.
     */
    private Map<String, Object> jsonToMap(String json) {
        Map<String, Object> map = new HashMap<>();

        try {
            // Use regex to extract key-value pairs
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|\\d+|true|false|null|\\{[^}]*\\}|\\[[^\\]]*\\])");
            Matcher matcher = pattern.matcher(json);

            while (matcher.find()) {
                String key = matcher.group(1);
                String valueStr = matcher.group(2);

                // Parse the value based on its format
                Object value;
                if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                    // String value
                    value = valueStr.substring(1, valueStr.length() - 1);
                } else if ("true".equals(valueStr) || "false".equals(valueStr)) {
                    // Boolean value
                    value = Boolean.parseBoolean(valueStr);
                } else if ("null".equals(valueStr)) {
                    // Null value
                    value = null;
                } else {
                    try {
                        // Number value
                        value = Integer.parseInt(valueStr);
                    } catch (NumberFormatException e1) {
                        try {
                            value = Double.parseDouble(valueStr);
                        } catch (NumberFormatException e2) {
                            // Just use the string as is
                            value = valueStr;
                        }
                    }
                }

                map.put(key, value);
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }

        return map;
    }
}