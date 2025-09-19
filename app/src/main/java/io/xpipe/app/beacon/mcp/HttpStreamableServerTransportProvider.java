/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.xpipe.app.beacon.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.KeepAliveScheduler;
import io.xpipe.app.issue.TrackEvent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class HttpStreamableServerTransportProvider implements McpStreamableServerTransportProvider {

    public static final String MESSAGE_EVENT_TYPE = "message";

    public static final String UTF_8 = "UTF-8";
    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_EVENT_STREAM = "text/event-stream";
    public static final String FAILED_TO_SEND_ERROR_RESPONSE = "Failed to send error response: {}";
    private static final Logger logger = LoggerFactory.getLogger(HttpStreamableServerTransportProvider.class);

    private static final String ACCEPT = "Accept";

    private final String mcpEndpoint;

    private final boolean disallowDelete;

    private final McpJsonMapper jsonMapper;

    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();

    private final McpTransportContextExtractor<HttpExchange> contextExtractor;
    private McpStreamableServerSession.Factory sessionFactory;

    private volatile boolean isClosing = false;

    private KeepAliveScheduler keepAliveScheduler;

    HttpStreamableServerTransportProvider(
            McpJsonMapper jsonMapper,
            String mcpEndpoint,
            boolean disallowDelete,
            McpTransportContextExtractor<HttpExchange> contextExtractor,
            Duration keepAliveInterval) {
        Assert.notNull(jsonMapper, "ObjectMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "Context extractor must not be null");

        this.jsonMapper = jsonMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.disallowDelete = disallowDelete;
        this.contextExtractor = contextExtractor;

        if (keepAliveInterval != null) {

            this.keepAliveScheduler = KeepAliveScheduler.builder(
                            () -> (isClosing) ? Flux.empty() : Flux.fromIterable(sessions.values()))
                    .initialDelay(keepAliveInterval)
                    .interval(keepAliveInterval)
                    .build();

            this.keepAliveScheduler.start();
        }
    }

    public List<String> protocolVersions() {
        return List.of(ProtocolVersions.MCP_2024_11_05, ProtocolVersions.MCP_2025_03_26, ProtocolVersions.MCP_2025_06_18);
    }

    @Override
    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (this.sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }

        logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());

        return Mono.fromRunnable(() -> {
            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    session.sendNotification(method, params).block();
                } catch (Exception e) {
                    logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }
            });
        });
    }

    /**
     * Initiates a graceful shutdown of the transport.
     *
     * @return A Mono that completes when all cleanup operations are finished
     */
    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
                    this.isClosing = true;
                    logger.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());

                    this.sessions.values().parallelStream().forEach(session -> {
                        try {
                            session.closeGracefully().block();
                        } catch (Exception e) {
                            logger.error("Failed to close session {}: {}", session.getId(), e.getMessage());
                        }
                    });

                    this.sessions.clear();
                    logger.debug("Graceful shutdown completed");
                })
                .then()
                .doOnSuccess(v -> {
                    sessions.clear();
                    logger.debug("Graceful shutdown completed");
                    if (this.keepAliveScheduler != null) {
                        this.keepAliveScheduler.shutdown();
                    }
                });
    }

    public void doGet(HttpExchange exchange) throws IOException {

        String requestURI = exchange.getRequestURI().toString();
        if (!requestURI.endsWith(mcpEndpoint)) {
            sendError(exchange, 404, null);
            return;
        }

        if (this.isClosing) {
            sendError(exchange, 503, "Server is shutting down");
            return;
        }

        List<String> badRequestErrors = new ArrayList<>();

        String accept = exchange.getRequestHeaders().getFirst(ACCEPT);
        if (accept == null || !accept.contains(TEXT_EVENT_STREAM)) {
            badRequestErrors.add("text/event-stream required in Accept header");
        }

        String sessionId = exchange.getRequestHeaders().getFirst(HttpHeaders.MCP_SESSION_ID);

        if (sessionId == null || sessionId.isBlank()) {
            badRequestErrors.add("Session ID required in mcp-session-id header");
        }

        if (!badRequestErrors.isEmpty()) {
            String combinedMessage = String.join("; ", badRequestErrors);
            this.sendError(exchange, 400, combinedMessage);
            return;
        }

        McpStreamableServerSession session = this.sessions.get(sessionId);

        if (session == null) {
            sendError(exchange, 404, null);
            return;
        }

        logger.debug("Handling GET request for session: {}", sessionId);

        McpTransportContext transportContext = this.contextExtractor.extract(exchange);

        try {
            exchange.getResponseHeaders().add("Content-Type", TEXT_EVENT_STREAM);
            exchange.getResponseHeaders().add("Content-Encoding", UTF_8);
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.getResponseHeaders().add("Connection", "keep-alive");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);

            var writer = new PrintWriter(exchange.getResponseBody());
            HttpServletStreamableMcpSessionTransport sessionTransport =
                    new HttpServletStreamableMcpSessionTransport(sessionId, exchange, writer);

            // Check if this is a replay request
            if (exchange.getRequestHeaders().getFirst(HttpHeaders.LAST_EVENT_ID) != null) {
                String lastId = exchange.getRequestHeaders().getFirst(HttpHeaders.LAST_EVENT_ID);

                try {
                    session.replay(lastId)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .toIterable()
                            .forEach(message -> {
                                try {
                                    sessionTransport
                                            .sendMessage(message)
                                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                                            .block();
                                } catch (Exception e) {
                                    logger.error("Failed to replay message: {}", e.getMessage());
                                    exchange.close();
                                }
                            });
                } catch (Exception e) {
                    logger.error("Failed to replay messages: {}", e.getMessage());
                    exchange.close();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to handle GET request for session {}: {}", sessionId, e.getMessage());
            sendError(exchange, 500, null);
        }
    }

    public void sendError(HttpExchange exchange, int code, String message) throws IOException {
        var b = message != null ? message.getBytes(StandardCharsets.UTF_8) : new byte[0];
        exchange.getResponseHeaders().add("Content-Encoding", UTF_8);
        exchange.sendResponseHeaders(code, b.length != 0 ? b.length : -1);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(b);
        }

        TrackEvent.error("MCP server error: " + message);
    }

    public void doPost(HttpExchange exchange) throws IOException {

        String requestURI = exchange.getRequestURI().toString();
        if (!requestURI.endsWith(mcpEndpoint)) {
            sendError(exchange, 404, null);
            return;
        }

        if (this.isClosing) {
            sendError(exchange, 503, "Server is shutting down");
            return;
        }

        List<String> badRequestErrors = new ArrayList<>();

        String accept = exchange.getRequestHeaders().getFirst(ACCEPT);
        if (accept == null || !accept.contains(TEXT_EVENT_STREAM)) {
            badRequestErrors.add("text/event-stream required in Accept header");
        }
        if (accept == null || !accept.contains(APPLICATION_JSON)) {
            badRequestErrors.add("application/json required in Accept header");
        }

        McpTransportContext transportContext = this.contextExtractor.extract(exchange);

        try {
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);

            // Handle initialization request
            if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest
                    && jsonrpcRequest.method().equals(McpSchema.METHOD_INITIALIZE)) {
                if (!badRequestErrors.isEmpty()) {
                    String combinedMessage = String.join("; ", badRequestErrors);
                    this.sendError(exchange, 400, combinedMessage);
                    return;
                }

                McpSchema.InitializeRequest initializeRequest = jsonMapper.convertValue(jsonrpcRequest.params(),
                        new TypeRef<McpSchema.InitializeRequest>() {});
                McpStreamableServerSession.McpStreamableServerSessionInit init =
                        this.sessionFactory.startSession(initializeRequest);
                this.sessions.put(init.session().getId(), init.session());

                try {
                    McpSchema.InitializeResult initResult = init.initResult().block();

                    String jsonResponse = jsonMapper.writeValueAsString(new McpSchema.JSONRPCResponse(
                            McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), initResult, null));
                    var jsonBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().add("Content-Type", APPLICATION_JSON);
                    exchange.getResponseHeaders().add("Content-Encoding", UTF_8);
                    exchange.getResponseHeaders()
                            .add(HttpHeaders.MCP_SESSION_ID, init.session().getId());
                    exchange.sendResponseHeaders(200, jsonBytes.length);
                    exchange.getResponseBody().write(jsonBytes);
                    return;
                } catch (Exception e) {
                    logger.error("Failed to initialize session: {}", e.getMessage());
                    this.sendError(exchange, 500, "Failed to initialize session: " + e.getMessage());
                    return;
                }
            }

            String sessionId = exchange.getRequestHeaders().getFirst(HttpHeaders.MCP_SESSION_ID);

            if (sessionId == null || sessionId.isBlank()) {
                badRequestErrors.add("Session ID required in mcp-session-id header");
            }

            if (!badRequestErrors.isEmpty()) {
                String combinedMessage = String.join("; ", badRequestErrors);
                this.sendError(exchange, 400, combinedMessage);
                return;
            }

            McpStreamableServerSession session = this.sessions.get(sessionId);

            if (session == null) {
                this.sendError(exchange, 404, "Session not found: " + sessionId + ". Was the session not refreshed?");
                return;
            }

            if (message instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
                session.accept(jsonrpcResponse)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                exchange.sendResponseHeaders(200, -1);
            } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
                session.accept(jsonrpcNotification)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                exchange.sendResponseHeaders(202, -1);
            } else if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
                // For streaming responses, we need to return SSE
                exchange.getResponseHeaders().add("Content-Type", TEXT_EVENT_STREAM);
                exchange.getResponseHeaders().add("Content-Encoding", UTF_8);
                exchange.getResponseHeaders().add("Cache-Control", "no-cache");
                exchange.getResponseHeaders().add("Connection", "keep-alive");
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, 0);

                var writer = new PrintWriter(exchange.getResponseBody());

                HttpServletStreamableMcpSessionTransport sessionTransport =
                        new HttpServletStreamableMcpSessionTransport(sessionId, exchange, writer);

                try {
                    session.responseStream(jsonrpcRequest, sessionTransport)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .block();
                } catch (Exception e) {
                    logger.error("Failed to handle request stream: {}", e.getMessage());
                    exchange.close();
                }
            } else {
                this.sendError(exchange, 500, "Unknown message type");
            }
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Failed to deserialize message: {}", e.getMessage());
            this.sendError(exchange, 400, "Invalid message format: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
            try {
                this.sendError(exchange, 500, "Error processing message: " + e.getMessage());
            } catch (IOException ex) {
                logger.error(FAILED_TO_SEND_ERROR_RESPONSE, ex.getMessage());
                sendError(exchange, 500, "Error processing message");
            }
        }
    }

    public void doOther(HttpExchange exchange) throws IOException {
        sendError(exchange, 405, "Unsupported HTTP method: " + exchange.getRequestMethod());
    }

    protected void doDelete(HttpExchange exchange) throws IOException {

        String requestURI = exchange.getRequestURI().toString();
        if (!requestURI.endsWith(mcpEndpoint)) {
            sendError(exchange, 404, null);
            return;
        }

        if (this.isClosing) {
            sendError(exchange, 503, "Server is shutting down");
            return;
        }

        if (this.disallowDelete) {
            sendError(exchange, 405, null);
            return;
        }

        McpTransportContext transportContext = this.contextExtractor.extract(exchange);

        if (exchange.getRequestHeaders().getFirst(HttpHeaders.MCP_SESSION_ID) == null) {
            sendError(exchange, 400, "Session ID required in mcp-session-id header");
            return;
        }

        String sessionId = exchange.getRequestHeaders().getFirst(HttpHeaders.MCP_SESSION_ID);
        McpStreamableServerSession session = this.sessions.get(sessionId);

        if (session == null) {
            sendError(exchange, 404, null);
            return;
        }

        try {
            session.delete()
                    .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                    .block();
            this.sessions.remove(sessionId);
            exchange.sendResponseHeaders(200, -1);
        } catch (Exception e) {
            logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
            try {
                sendError(exchange, 500, e.getMessage());
            } catch (IOException ex) {
                logger.error(FAILED_TO_SEND_ERROR_RESPONSE, ex.getMessage());
                sendError(exchange, 500, "Error deleting session");
            }
        }
    }

    private void sendEvent(PrintWriter writer, String eventType, String data, String id) throws IOException {
        if (id != null) {
            writer.write("id: " + id + "\n");
        }
        writer.write("event: " + eventType + "\n");
        writer.write("data: " + data + "\n\n");
        writer.flush();

        if (writer.checkError()) {
            throw new IOException("Client disconnected");
        }
    }

    private class HttpServletStreamableMcpSessionTransport implements McpStreamableServerTransport {

        private final String sessionId;

        private final HttpExchange exchange;

        private final PrintWriter writer;
        private final ReentrantLock lock = new ReentrantLock();
        private volatile boolean closed = false;

        HttpServletStreamableMcpSessionTransport(String sessionId, HttpExchange exchange, PrintWriter writer) {
            this.sessionId = sessionId;
            this.exchange = exchange;
            this.writer = writer;
            logger.debug("Streamable session transport {} initialized with SSE writer", sessionId);
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return jsonMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return Mono.fromRunnable(() -> {
                if (this.closed) {
                    logger.debug("Attempted to send message to closed session: {}", this.sessionId);
                    return;
                }

                lock.lock();
                try {
                    if (this.closed) {
                        logger.debug("Session {} was closed during message send attempt", this.sessionId);
                        return;
                    }

                    String jsonText = jsonMapper.writeValueAsString(message);
                    HttpStreamableServerTransportProvider.this.sendEvent(
                            writer, MESSAGE_EVENT_TYPE, jsonText, messageId != null ? messageId : this.sessionId);
                    logger.debug("Message sent to session {} with ID {}", this.sessionId, messageId);
                } catch (Exception e) {
                    logger.error("Failed to send message to session {}: {}", this.sessionId, e.getMessage());
                    HttpStreamableServerTransportProvider.this.sessions.remove(this.sessionId);
                    exchange.close();
                } finally {
                    lock.unlock();
                }
            });
        }

        @Override
        public void close() {
            lock.lock();
            try {
                if (this.closed) {
                    logger.debug("Session transport {} already closed", this.sessionId);
                    return;
                }

                this.closed = true;

                // HttpServletStreamableServerTransportProvider.this.sessions.remove(this.sessionId);
                exchange.close();
                logger.debug("Successfully completed async context for session {}", sessionId);
            } catch (Exception e) {
                logger.warn("Failed to complete async context for session {}: {}", sessionId, e.getMessage());
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                HttpServletStreamableMcpSessionTransport.this.close();
            });
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return sendMessage(message, null);
        }
    }
}
