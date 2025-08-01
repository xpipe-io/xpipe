/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.xpipe.app.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import io.modelcontextprotocol.server.DefaultMcpTransportContext;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.KeepAliveScheduler;
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

	private static final Logger logger = LoggerFactory.getLogger(HttpStreamableServerTransportProvider.class);

	/**
	 * Event type for JSON-RPC messages sent through the SSE connection.
	 */
	public static final String MESSAGE_EVENT_TYPE = "message";

	/**
	 * Event type for sending the message endpoint URI to clients.
	 */
	public static final String ENDPOINT_EVENT_TYPE = "endpoint";

	/**
	 * Header name for the response media types accepted by the requester.
	 */
	private static final String ACCEPT = "Accept";

	public static final String UTF_8 = "UTF-8";

	public static final String APPLICATION_JSON = "application/json";

	public static final String TEXT_EVENT_STREAM = "text/event-stream";

	public static final String FAILED_TO_SEND_ERROR_RESPONSE = "Failed to send error response: {}";

	/**
	 * The endpoint URI where clients should send their JSON-RPC messages. Defaults to
	 * "/mcp".
	 */
	private final String mcpEndpoint;

	/**
	 * Flag indicating whether DELETE requests are disallowed on the endpoint.
	 */
	private final boolean disallowDelete;

	private final ObjectMapper objectMapper;

	private McpStreamableServerSession.Factory sessionFactory;

	/**
	 * Map of active client sessions, keyed by mcp-session-id.
	 */
	private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();

	private McpTransportContextExtractor<HttpExchange> contextExtractor;

	/**
	 * Flag indicating if the transport is shutting down.
	 */
	private volatile boolean isClosing = false;

	/**
	 * Keep-alive scheduler for managing session pings. Activated if keepAliveInterval is
	 * set. Disabled by default.
	 */
	private KeepAliveScheduler keepAliveScheduler;

	/**
	 * Constructs a new HttpServletStreamableServerTransportProvider instance.
	 * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization
	 * of messages.
	 * @param mcpEndpoint The endpoint URI where clients should send their JSON-RPC
	 * messages via HTTP. This endpoint will handle GET, POST, and DELETE requests.
	 * @param disallowDelete Whether to disallow DELETE requests on the endpoint.
	 * @param contextExtractor The extractor for transport context from the request.
	 * @throws IllegalArgumentException if any parameter is null
	 */
	private HttpStreamableServerTransportProvider(ObjectMapper objectMapper, String mcpEndpoint,
												  boolean disallowDelete, McpTransportContextExtractor<HttpExchange> contextExtractor,
												  Duration keepAliveInterval) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
		Assert.notNull(contextExtractor, "Context extractor must not be null");

		this.objectMapper = objectMapper;
		this.mcpEndpoint = mcpEndpoint;
		this.disallowDelete = disallowDelete;
		this.contextExtractor = contextExtractor;

		if (keepAliveInterval != null) {

			this.keepAliveScheduler = KeepAliveScheduler
				.builder(() -> (isClosing) ? Flux.empty() : Flux.fromIterable(sessions.values()))
				.initialDelay(keepAliveInterval)
				.interval(keepAliveInterval)
				.build();

			this.keepAliveScheduler.start();
		}

	}

	@Override
	public String protocolVersion() {
		return "2025-03-26";
	}

	@Override
	public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Broadcasts a notification to all connected clients through their SSE connections.
	 * If any errors occur during sending to a particular client, they are logged but
	 * don't prevent sending to other clients.
	 * @param method The method name for the notification
	 * @param params The parameters for the notification
	 * @return A Mono that completes when the broadcast attempt is finished
	 */
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
				}
				catch (Exception e) {
					logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
				}
			});
		});
	}

	/**
	 * Initiates a graceful shutdown of the transport.
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
				}
				catch (Exception e) {
					logger.error("Failed to close session {}: {}", session.getId(), e.getMessage());
				}
			});

			this.sessions.clear();
			logger.debug("Graceful shutdown completed");
		}).then().doOnSuccess(v -> {
			sessions.clear();
			logger.debug("Graceful shutdown completed");
			if (this.keepAliveScheduler != null) {
				this.keepAliveScheduler.shutdown();
			}
		});
	}

	public void doGet(HttpExchange exchange)
			throws IOException {

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

		McpTransportContext transportContext = this.contextExtractor.extract(exchange, new DefaultMcpTransportContext());

		try {
			exchange.getResponseHeaders().add("Content-Type", TEXT_EVENT_STREAM);
			exchange.getResponseHeaders().add("Content-Encoding", UTF_8);
			exchange.getResponseHeaders().add("Cache-Control", "no-cache");
			exchange.getResponseHeaders().add("Connection", "keep-alive");
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(200, 0);

			var writer = new PrintWriter(exchange.getResponseBody());
			HttpServletStreamableMcpSessionTransport sessionTransport = new HttpServletStreamableMcpSessionTransport(
					sessionId, exchange, writer);

			// Check if this is a replay request
			if (exchange.getRequestHeaders().getFirst(HttpHeaders.LAST_EVENT_ID) != null) {
				String lastId = exchange.getRequestHeaders().getFirst(HttpHeaders.LAST_EVENT_ID);

				try {
					session.replay(lastId)
						.contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
						.toIterable()
						.forEach(message -> {
							try {
								sessionTransport.sendMessage(message)
									.contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
									.block();
							}
							catch (Exception e) {
								logger.error("Failed to replay message: {}", e.getMessage());
								exchange.close();
							}
						});
				}
				catch (Exception e) {
					logger.error("Failed to replay messages: {}", e.getMessage());
					exchange.close();
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to handle GET request for session {}: {}", sessionId, e.getMessage());
			sendError(exchange, 500, null);
		}
	}

	private void sendError(HttpExchange exchange, int code, String message) throws IOException {
		var b = message != null ? message.getBytes(StandardCharsets.UTF_8) : new byte[0];
		exchange.getResponseHeaders().add("Content-Encoding", UTF_8);
		exchange.sendResponseHeaders(code, b.length != 0 ? b.length : -1);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(b);
		}
	}

	public void doPost(HttpExchange exchange)
			throws IOException {

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

		McpTransportContext transportContext = this.contextExtractor.extract(exchange, new DefaultMcpTransportContext());

		try {
			var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

			McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

			// Handle initialization request
			if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest
					&& jsonrpcRequest.method().equals(McpSchema.METHOD_INITIALIZE)) {
				if (!badRequestErrors.isEmpty()) {
					String combinedMessage = String.join("; ", badRequestErrors);
					this.sendError(exchange, 400, combinedMessage);
					return;
				}

				McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(jsonrpcRequest.params(),
						new TypeReference<McpSchema.InitializeRequest>() {
						});
				McpStreamableServerSession.McpStreamableServerSessionInit init = this.sessionFactory
					.startSession(initializeRequest);
				this.sessions.put(init.session().getId(), init.session());

				try {
					McpSchema.InitializeResult initResult = init.initResult().block();

					String jsonResponse = objectMapper.writeValueAsString(new McpSchema.JSONRPCResponse(
							McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), initResult, null));
					var jsonBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

					exchange.getResponseHeaders().add("Content-Type", APPLICATION_JSON);
					exchange.getResponseHeaders().add("Content-Encoding", UTF_8);
					exchange.getResponseHeaders().add(HttpHeaders.MCP_SESSION_ID, init.session().getId());
					exchange.sendResponseHeaders(200, jsonBytes.length);
					exchange.getResponseBody().write(jsonBytes);
					return;
				}
				catch (Exception e) {
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
			}
			else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
				session.accept(jsonrpcNotification)
					.contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
					.block();
				exchange.sendResponseHeaders(202, -1);
			}
			else if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
				// For streaming responses, we need to return SSE
				exchange.getResponseHeaders().add("Content-Type", TEXT_EVENT_STREAM);
				exchange.getResponseHeaders().add("Content-Encoding", UTF_8);
				exchange.getResponseHeaders().add("Cache-Control", "no-cache");
				exchange.getResponseHeaders().add("Connection", "keep-alive");
				exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
				exchange.sendResponseHeaders(200, 0);

				var writer = new PrintWriter(exchange.getResponseBody());

				HttpServletStreamableMcpSessionTransport sessionTransport = new HttpServletStreamableMcpSessionTransport(
						sessionId, exchange, writer);

				try {
					session.responseStream(jsonrpcRequest, sessionTransport)
						.contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
						.block();
				}
				catch (Exception e) {
					logger.error("Failed to handle request stream: {}", e.getMessage());
					exchange.close();
				}
			}
			else {
				this.sendError(exchange, 500, "Unknown message type");
			}
		}
		catch (IllegalArgumentException | IOException e) {
			logger.error("Failed to deserialize message: {}", e.getMessage());
			this.sendError(exchange, 400, "Invalid message format: " + e.getMessage());
		}
		catch (Exception e) {
			logger.error("Error handling message: {}", e.getMessage());
			try {
				this.sendError(exchange, 500, "Error processing message: " + e.getMessage());
			}
			catch (IOException ex) {
				logger.error(FAILED_TO_SEND_ERROR_RESPONSE, ex.getMessage());
				sendError(exchange, 500, "Error processing message");
			}
		}
	}


	public void doOther(HttpExchange exchange)
			throws IOException {
		sendError(exchange, 405, "Unsupported HTTP method: " + exchange.getRequestMethod());
	}
//
//	/**
//	 * Handles DELETE requests for session deletion.
//	 * @param request The HTTP servlet request
//	 * @param response The HTTP servlet response
//	 * @throws ServletException If a servlet-specific error occurs
//	 * @throws IOException If an I/O error occurs
//	 */
//	@Override
//	protected void doDelete(HttpRequest request, HttpServletResponse response)
//			throws IOException {
//
//		String requestURI = request.getRequestURI();
//		if (!requestURI.endsWith(mcpEndpoint)) {
//			response.sendError(404);
//			return;
//		}
//
//		if (this.isClosing) {
//			response.sendError(503, "Server is shutting down");
//			return;
//		}
//
//		if (this.disallowDelete) {
//			response.sendError(405);
//			return;
//		}
//
//		McpTransportContext transportContext = this.contextExtractor.extract(request, new DefaultMcpTransportContext());
//
//		if (request.headers().firstValue(HttpHeaders.MCP_SESSION_ID).orElse(null) == null) {
//			this.responseError(response, 400,
//					new McpError("Session ID required in mcp-session-id header"));
//			return;
//		}
//
//		String sessionId = request.headers().firstValue(HttpHeaders.MCP_SESSION_ID).orElse(null);
//		McpStreamableServerSession session = this.sessions.get(sessionId);
//
//		if (session == null) {
//			response.sendError(404);
//			return;
//		}
//
//		try {
//			session.delete().contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)).block();
//			this.sessions.remove(sessionId);
//			response.setStatus(200);
//		}
//		catch (Exception e) {
//			logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
//			try {
//				this.responseError(response, 500,
//						new McpError(e.getMessage()));
//			}
//			catch (IOException ex) {
//				logger.error(FAILED_TO_SEND_ERROR_RESPONSE, ex.getMessage());
//				response.sendError(500, "Error deleting session");
//			}
//		}
//	}

	/**
	 * Sends an SSE event to a client with a specific ID.
	 * @param writer The writer to send the event through
	 * @param eventType The type of event (message or endpoint)
	 * @param data The event data
	 * @param id The event ID
	 * @throws IOException If an error occurs while writing the event
	 */
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

	/**
	 * Implementation of McpStreamableServerTransport for HttpServlet SSE sessions. This
	 * class handles the transport-level communication for a specific client session.
	 *
	 * <p>
	 * This class is thread-safe and uses a ReentrantLock to synchronize access to the
	 * underlying PrintWriter to prevent race conditions when multiple threads attempt to
	 * send messages concurrently.
	 */

	private class HttpServletStreamableMcpSessionTransport implements McpStreamableServerTransport {

		private final String sessionId;

		private final HttpExchange exchange;

		private final PrintWriter writer;

		private volatile boolean closed = false;

		private final ReentrantLock lock = new ReentrantLock();

		HttpServletStreamableMcpSessionTransport(String sessionId, HttpExchange exchange, PrintWriter writer) {
			this.sessionId = sessionId;
			this.exchange = exchange;
			this.writer = writer;
			logger.debug("Streamable session transport {} initialized with SSE writer", sessionId);
		}

		/**
		 * Sends a JSON-RPC message to the client through the SSE connection.
		 * @param message The JSON-RPC message to send
		 * @return A Mono that completes when the message has been sent
		 */
		@Override
		public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
			return sendMessage(message, null);
		}

		/**
		 * Sends a JSON-RPC message to the client through the SSE connection with a
		 * specific message ID.
		 * @param message The JSON-RPC message to send
		 * @param messageId The message ID for SSE event identification
		 * @return A Mono that completes when the message has been sent
		 */
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

					String jsonText = objectMapper.writeValueAsString(message);
					HttpStreamableServerTransportProvider.this.sendEvent(writer, MESSAGE_EVENT_TYPE, jsonText,
							messageId != null ? messageId : this.sessionId);
					logger.debug("Message sent to session {} with ID {}", this.sessionId, messageId);
				}
				catch (Exception e) {
					logger.error("Failed to send message to session {}: {}", this.sessionId, e.getMessage());
					HttpStreamableServerTransportProvider.this.sessions.remove(this.sessionId);
					exchange.close();
				}
				finally {
					lock.unlock();
				}
			});
		}

		/**
		 * Converts data from one type to another using the configured ObjectMapper.
		 * @param data The source data object to convert
		 * @param typeRef The target type reference
		 * @return The converted object of type T
		 * @param <T> The target type
		 */
		@Override
		public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
			return objectMapper.convertValue(data, typeRef);
		}

		/**
		 * Initiates a graceful shutdown of the transport.
		 * @return A Mono that completes when the shutdown is complete
		 */
		@Override
		public Mono<Void> closeGracefully() {
			return Mono.fromRunnable(() -> {
				HttpServletStreamableMcpSessionTransport.this.close();
			});
		}

		/**
		 * Closes the transport immediately.
		 */
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
			}
			catch (Exception e) {
				logger.warn("Failed to complete async context for session {}: {}", sessionId, e.getMessage());
			}
			finally {
				lock.unlock();
			}
		}

	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for creating instances of
	 * {@link HttpStreamableServerTransportProvider}.
	 */
	public static class Builder {

		private ObjectMapper objectMapper;

		private String mcpEndpoint = "/mcp";

		private boolean disallowDelete = false;

		private McpTransportContextExtractor<HttpExchange> contextExtractor = (serverRequest, context) -> context;

		private Duration keepAliveInterval;

		/**
		 * Sets the ObjectMapper to use for JSON serialization/deserialization of MCP
		 * messages.
		 * @param objectMapper The ObjectMapper instance. Must not be null.
		 * @return this builder instance
		 * @throws IllegalArgumentException if objectMapper is null
		 */
		public Builder objectMapper(ObjectMapper objectMapper) {
			Assert.notNull(objectMapper, "ObjectMapper must not be null");
			this.objectMapper = objectMapper;
			return this;
		}

		/**
		 * Sets the endpoint URI where clients should send their JSON-RPC messages.
		 * @param mcpEndpoint The MCP endpoint URI. Must not be null.
		 * @return this builder instance
		 * @throws IllegalArgumentException if mcpEndpoint is null
		 */
		public Builder mcpEndpoint(String mcpEndpoint) {
			Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
			this.mcpEndpoint = mcpEndpoint;
			return this;
		}

		/**
		 * Sets whether to disallow DELETE requests on the endpoint.
		 * @param disallowDelete true to disallow DELETE requests, false otherwise
		 * @return this builder instance
		 */
		public Builder disallowDelete(boolean disallowDelete) {
			this.disallowDelete = disallowDelete;
			return this;
		}

		/**
		 * Sets the context extractor for extracting transport context from the request.
		 * @param contextExtractor The context extractor to use. Must not be null.
		 * @return this builder instance
		 * @throws IllegalArgumentException if contextExtractor is null
		 */
		public Builder contextExtractor(McpTransportContextExtractor<HttpExchange> contextExtractor) {
			Assert.notNull(contextExtractor, "Context extractor must not be null");
			this.contextExtractor = contextExtractor;
			return this;
		}

		/**
		 * Sets the keep-alive interval for the transport. If set, a keep-alive scheduler
		 * will be activated to periodically ping active sessions.
		 * @param keepAliveInterval The interval for keep-alive pings. If null, no
		 * keep-alive will be scheduled.
		 * @return this builder instance
		 */
		public Builder keepAliveInterval(Duration keepAliveInterval) {
			this.keepAliveInterval = keepAliveInterval;
			return this;
		}

		/**
		 * Builds a new instance of {@link HttpStreamableServerTransportProvider}
		 * with the configured settings.
		 * @return A new HttpServletStreamableServerTransportProvider instance
		 * @throws IllegalStateException if required parameters are not set
		 */
		public HttpStreamableServerTransportProvider build() {
			Assert.notNull(this.objectMapper, "ObjectMapper must be set");
			Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");

			return new HttpStreamableServerTransportProvider(this.objectMapper, this.mcpEndpoint,
					this.disallowDelete, this.contextExtractor, this.keepAliveInterval);
		}

	}

}
