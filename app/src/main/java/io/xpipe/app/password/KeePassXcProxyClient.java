package io.xpipe.app.password;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for communicating with KeePassXC using the native messaging protocol.
 * This implementation communicates with the actual running KeePassXC-proxy process
 * via stdin and stdout.
 *
 * Native messaging uses length-prefixed JSON messages over stdin/stdout.
 */
public class KeePassXcProxyClient {

    // Default timeouts for different operations (milliseconds)
    private static final long TIMEOUT_ASSOCIATE = 30000; // Associate needs user interaction
    private static final long TIMEOUT_GET_LOGINS = 5000; // Getting logins is usually fast
    private static final long TIMEOUT_TEST_ASSOCIATE = 2000; // Testing association is quick

    private final Path proxyExecutable;
    private Process process;
    private String clientId;
    private TweetNaClHelper.KeyPair keyPair;
    private byte[] serverPublicKey;

    @Getter
    private KeePassXcAssociationKey associationKey;

    public KeePassXcProxyClient(Path proxyExecutable) {
        this.proxyExecutable = proxyExecutable;
    }

    /**
     * Extracts the action from a JSON response.
     *
     * @param response The JSON response
     * @return The action, or null if not found
     */
    private String extractAction(String response) {
        Pattern pattern = Pattern.compile("\"action\":\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public void useExistingAssociationKey(KeePassXcAssociationKey key) {
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

        var ex = new IllegalStateException(
                "KeePassXC client did not respond. Is the browser integration enabled for your KeePassXC database?");
        ErrorEvent.preconfigure(ErrorEvent.fromThrowable(ex).expected().documentationLink(DocumentationLink.KEEPASSXC));
        throw ex;
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
        messageData.put("key", associationKey.getKey().getSecretValue());

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

        // Parse and decrypt the response
        Map<String, Object> responseMap = jsonToMap(responseJson);

        if (responseMap.containsKey("error")) {
            throw ErrorEvent.expected(
                    new IllegalStateException(responseMap.get("error").toString()));
        }

        if (responseMap.containsKey("message") && responseMap.containsKey("nonce")) {
            String encryptedResponse = (String) responseMap.get("message");
            String responseNonce = (String) responseMap.get("nonce");

            String decryptedResponse = decrypt(encryptedResponse, responseNonce);
            if (decryptedResponse != null) {
                Map<String, Object> parsedResponse = jsonToMap(decryptedResponse);
                boolean success = parsedResponse.containsKey("success")
                        && "true".equals(parsedResponse.get("success").toString());
                if (!success) {
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
    public String getLoginsMessage(String url) throws IOException {
        // Generate a nonce
        String nonce = TweetNaClHelper.encodeBase64(TweetNaClHelper.randomBytes(TweetNaClHelper.NONCE_SIZE));

        // Create the unencrypted message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("action", "get-logins");
        messageData.put("url", url);

        // Add the keys
        Map<String, Object> keyData = new HashMap<>();
        keyData.put("id", associationKey.getId());
        keyData.put("key", associationKey.getKey().getSecretValue());

        messageData.put("keys", new Map[] {keyData});

        // Encrypt the message
        String encryptedMessage = encrypt(messageData, nonce);

        // Build the request
        Map<String, Object> request = new HashMap<>();
        request.put("action", "get-logins");
        request.put("message", encryptedMessage);
        request.put("nonce", nonce);
        request.put("clientID", clientId);

        String requestJson = mapToJson(request);

        // Send the request
        String responseJson = sendRequest("get-logins", requestJson, TIMEOUT_GET_LOGINS);

        Map<String, Object> responseMap = jsonToMap(responseJson);
        if (responseMap.containsKey("error")) {
            throw ErrorEvent.expected(
                    new IllegalStateException(responseMap.get("error").toString()));
        }

        if (responseMap.containsKey("message") && responseMap.containsKey("nonce")) {
            String encryptedResponse = (String) responseMap.get("message");
            String responseNonce = (String) responseMap.get("nonce");
            return decrypt(encryptedResponse, responseNonce);
        }

        throw new IllegalStateException("Login query failed for an unknown reason");
    }

    public PasswordManager.CredentialResult getCredentials(String message) throws IOException {
        var tree = JacksonMapper.getDefault().readTree(message);
        var count = tree.required("count").asInt();
        if (count == 0) {
            throw ErrorEvent.expected(new IllegalArgumentException("No password was found for specified key"));
        }

        if (count > 1) {
            throw ErrorEvent.expected(
                    new IllegalArgumentException("Password key is ambiguous and returned multiple results"));
        }

        var object = (ObjectNode) tree.required("entries").get(0);
        var usernameField = object.required("login").asText();
        var passwordField = object.required("password").asText();
        return new PasswordManager.CredentialResult(usernameField.isEmpty() ? null : usernameField, passwordField.isEmpty() ? null : InPlaceSecretValue.of(passwordField));
    }

    /**
     * Disconnects from KeePassXC.
     */
    public void disconnect() {
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
        // Send the message
        sendNativeMessage(message);

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            var response = receiveNativeMessage();
            if (filterResponse(action, response)) {
                continue;
            }

            return response;
        }
        throw new IllegalStateException("KeePassXC " + action + " request timed out");
    }

    private boolean filterResponse(String action, String response) {
        // Extract action
        String extractedAction = extractAction(response);

        // Special handling for action-specific responses
        if ("database-locked".equals(extractedAction) || "database-unlocked".equals(extractedAction)) {
            // Update state based on the action
            if ("database-locked".equals(extractedAction)) {
                return true;
            }
        }

        if (action.equals(extractedAction)) {
            return false;
        } else {
            return true;
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
     * Encrypts a message for sending to KeePassXC.
     *
     * @param message The message to encrypt
     * @param nonce The nonce to use for encryption
     * @return The encrypted message, or null if encryption failed
     */
    private String encrypt(Map<String, Object> message, String nonce) {
        String messageJson = mapToJson(message);
        byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);
        byte[] nonceBytes = TweetNaClHelper.decodeBase64(nonce);

        byte[] encrypted = TweetNaClHelper.box(messageBytes, nonceBytes, serverPublicKey, keyPair.getSecretKey());

        return TweetNaClHelper.encodeBase64(encrypted);
    }

    /**
     * Decrypts a message received from KeePassXC.
     *
     * @param encryptedMessage The encrypted message
     * @param nonce The nonce used for encryption
     * @return The decrypted message, or null if decryption failed
     */
    private String decrypt(String encryptedMessage, String nonce) {
        byte[] messageBytes = TweetNaClHelper.decodeBase64(encryptedMessage);
        byte[] nonceBytes = TweetNaClHelper.decodeBase64(nonce);

        byte[] decrypted = TweetNaClHelper.boxOpen(messageBytes, nonceBytes, serverPublicKey, keyPair.getSecretKey());

        if (decrypted == null) {
            throw new IllegalArgumentException("Message decryption failed");
        }

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Associate with KeePassXC.
     *
     * @return True if successful, false otherwise
     * @throws IOException If there's an error communicating with KeePassXC
     */
    public void associate() throws IOException {
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

        // Build the request
        Map<String, Object> request = new HashMap<>();
        request.put("action", "associate");
        request.put("message", encryptedMessage);
        request.put("nonce", nonce);
        request.put("clientID", clientId);

        String requestJson = mapToJson(request);
        // Send the request using longer timeout as it requires user interaction
        String responseJson = sendRequest("associate", requestJson, TIMEOUT_ASSOCIATE);

        Map<String, Object> responseMap = jsonToMap(responseJson);

        if (responseMap.containsKey("error")) {
            throw ErrorEvent.expected(
                    new IllegalStateException(responseMap.get("error").toString()));
        }

        if (responseMap.containsKey("message") && responseMap.containsKey("nonce")) {
            String encryptedResponse = (String) responseMap.get("message");
            String responseNonce = (String) responseMap.get("nonce");

            String decryptedResponse = decrypt(encryptedResponse, responseNonce);
            Map<String, Object> parsedResponse = jsonToMap(decryptedResponse);
            boolean success = parsedResponse.containsKey("success")
                    && "true".equals(parsedResponse.get("success").toString());

            if (success && parsedResponse.containsKey("id") && parsedResponse.containsKey("hash")) {
                String id = (String) parsedResponse.get("id");
                var key = InPlaceSecretValue.of(TweetNaClHelper.encodeBase64(idKeyPair.getPublicKey()));

                associationKey = new KeePassXcAssociationKey(id, key);

                return;
            }
        }

        throw new IllegalStateException("KeePassXC association failed");
    }

    /**
     * Convert a map to a JSON string.
     */
    @SneakyThrows
    private String mapToJson(Map<String, Object> map) {
        var mapper = JacksonMapper.getDefault();
        return mapper.writeValueAsString(map);
    }

    /**
     * Convert a JSON string to a map.
     */
    @SneakyThrows
    private Map<String, Object> jsonToMap(String json) {
        var mapper = JacksonMapper.getDefault();
        var type = TypeFactory.defaultInstance().constructType(new TypeReference<>() {});
        Map<String, Object> map = mapper.readValue(json, type);
        return map;
    }
}
