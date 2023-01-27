package io.xpipe.app.core;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.xpipe.app.exchange.MessageExchangeImpls;
import io.xpipe.beacon.*;
import io.xpipe.beacon.exchange.MessageExchanges;
import io.xpipe.beacon.exchange.data.ClientErrorMessage;
import io.xpipe.beacon.exchange.data.ServerErrorMessage;
import io.xpipe.core.util.Deobfuscator;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class AppSocketServer {

    private static AppSocketServer INSTANCE;
    private final int port;
    private ServerSocket socket;
    private boolean running;
    private int connectionCounter;
    private Thread listenerThread;

    private AppSocketServer(int port) {
        this.port = port;
    }

    public static void init() {
        try {
            var port = BeaconConfig.getUsedPort();
            INSTANCE = new AppSocketServer(port);
            INSTANCE.createSocketListener();

            TrackEvent.withInfo("Initialized socket server")
                    .tag("port", port)
                    .build()
                    .handle();
        } catch (Exception ex) {
            // Not terminal!
            ErrorEvent.fromThrowable(ex).build().handle();
        }
    }

    public static void reset() {
        if (INSTANCE != null) {
            INSTANCE.stop();
            INSTANCE = null;
        }
    }

    private void stop() {
        if (!running) {
            return;
        }

        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
        }
        try {
            listenerThread.join();
        } catch (InterruptedException e) {
        }
    }

    private void createSocketListener() throws IOException {
        socket = new ServerSocket(port, 10000, InetAddress.getLoopbackAddress());
        running = true;
        listenerThread = new Thread(
                () -> {
                    while (running) {
                        Socket clientSocket = null;
                        try {
                            clientSocket = socket.accept();
                        } catch (Exception ex) {
                            continue;
                        }

                        try {
                            performExchangesAsync(clientSocket);
                        } catch (Exception ex) {
                            ErrorEvent.fromThrowable(ex).build().handle();
                        }
                        connectionCounter++;
                    }
                },
                "socket server");
        listenerThread.start();
    }

    private boolean performExchange(Socket clientSocket, int id) throws Exception {
        if (clientSocket.isClosed()) {
            TrackEvent.trace("beacon", "Socket closed");
            return false;
        }

        JsonNode node = null;
        try (InputStream blockIn = BeaconFormat.readBlocks(clientSocket.getInputStream())) {
            node = JacksonMapper.newMapper().readTree(blockIn);
        }
        if (node.isMissingNode()) {
            TrackEvent.trace("beacon", "Received EOF");
            return false;
        }

        TrackEvent.trace("beacon", "Received raw request: \n" + node.toPrettyString());

        var req = parseRequest(node);
        TrackEvent.trace("beacon", "Parsed request: \n" + req.toString());

        var prov = MessageExchangeImpls.byRequest(req);
        if (prov.isEmpty()) {
            throw new IllegalArgumentException("Unknown request id: " + req.getClass());
        }
        AtomicReference<BeaconClient.FailableRunnable<Exception>> post = new AtomicReference<>();
        var res = prov.get()
                .handleRequest(
                        new BeaconHandler() {
                            @Override
                            public void postResponse(BeaconClient.FailableRunnable<Exception> r) {
                                post.set(r);
                            }

                            @Override
                            public OutputStream sendBody() throws IOException {
                                TrackEvent.trace("beacon", "Starting writing body for #" + id);
                                return AppSocketServer.this.sendBody(clientSocket);
                            }

                            @Override
                            public InputStream receiveBody() throws IOException {
                                TrackEvent.trace("beacon", "Starting to read body for #" + id);
                                return AppSocketServer.this.receiveBody(clientSocket);
                            }
                        },
                        req);

        TrackEvent.trace("beacon", "Sending response to #" + id + ": \n" + res.toString());
        AppSocketServer.this.sendResponse(clientSocket, res);

        try {
            // If this fails, we sadly can't send an error response. Therefore just report it on the server side
            if (post.get() != null) {
                post.get().run();
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }

        TrackEvent.builder()
                .category("beacon")
                .type("trace")
                .message("Socket connection #" + id + " performed exchange "
                        + req.getClass().getSimpleName())
                .build()
                .handle();

        return true;
    }

    private void performExchanges(Socket clientSocket, int id) {
        try {
            JsonNode informationNode = null;
            try (InputStream blockIn = BeaconFormat.readBlocks(clientSocket.getInputStream())) {
                informationNode = JacksonMapper.newMapper().readTree(blockIn);
            }
            if (informationNode.isMissingNode()) {
                TrackEvent.trace("beacon", "Received EOF");
                return;
            }
            var information =
                    JacksonMapper.newMapper().treeToValue(informationNode, BeaconClient.ClientInformation.class);

            TrackEvent.builder()
                    .category("beacon")
                    .type("trace")
                    .message("Created new socket connection #" + id)
                    .tag("client", information != null ? information.toDisplayString() : "Unknown")
                    .build()
                    .handle();

            try {
                while (true) {
                    if (!performExchange(clientSocket, id)) {
                        break;
                    }
                }
                TrackEvent.builder()
                        .category("beacon")
                        .type("trace")
                        .message("Socket connection #" + id + " finished successfully")
                        .build()
                        .handle();

            } catch (ClientException ce) {
                TrackEvent.trace("beacon", "Sending client error to #" + id + ": " + ce.getMessage());
                sendClientErrorResponse(clientSocket, ce.getMessage());
            } catch (ServerException se) {
                TrackEvent.trace("beacon", "Sending server error to #" + id + ": " + se.getMessage());
                ErrorEvent.fromThrowable(se).build().handle();
                Deobfuscator.deobfuscate(se);
                sendServerErrorResponse(clientSocket, se);
            } catch (SocketException ex) {
                // Do not send error and omit it, as this might happen often
                // We do not send the error as the socket connection might be broken
                ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
            } catch (Throwable ex) {
                TrackEvent.trace("beacon", "Sending internal server error to #" + id + ": " + ex.getMessage());
                ErrorEvent.fromThrowable(ex).build().handle();
                Deobfuscator.deobfuscate(ex);
                sendServerErrorResponse(clientSocket, ex);
            }
        } catch (SocketException ex) {
            // Omit it, as this might happen often
            ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).build().handle();
        } finally {
            try {
                clientSocket.close();
                TrackEvent.trace("beacon", "Closed socket #" + id);
            } catch (IOException e) {
                ErrorEvent.fromThrowable(e).build().handle();
            }
        }

        TrackEvent.builder()
                .category("beacon")
                .type("trace")
                .message("Socket connection #" + id + " finished unsuccessfully");
    }

    private void performExchangesAsync(Socket clientSocket) {
        var id = connectionCounter;
        var t = new Thread(
                () -> {
                    performExchanges(clientSocket, id);
                },
                "socket connection #" + id);
        t.start();
    }

    public OutputStream sendBody(Socket outSocket) throws IOException {
        outSocket.getOutputStream().write(BeaconConfig.BODY_SEPARATOR);
        return BeaconFormat.writeBlocks(outSocket.getOutputStream());
    }

    public InputStream receiveBody(Socket outSocket) throws IOException {
        var read = outSocket.getInputStream().readNBytes(BeaconConfig.BODY_SEPARATOR.length);
        if (!Arrays.equals(read, BeaconConfig.BODY_SEPARATOR)) {
            throw new IOException("Expected body start (" + HexFormat.of().formatHex(BeaconConfig.BODY_SEPARATOR)
                    + ") but got " + HexFormat.of().formatHex(read));
        }
        return BeaconFormat.readBlocks(outSocket.getInputStream());
    }

    public <T extends ResponseMessage> void sendResponse(Socket outSocket, T obj) throws Exception {
        ObjectNode json = JacksonMapper.newMapper().valueToTree(obj);
        var prov = MessageExchanges.byResponse(obj).get();
        json.set("messageType", new TextNode(prov.getId()));
        json.set("messagePhase", new TextNode("response"));
        var msg = JsonNodeFactory.instance.objectNode();
        msg.set("xPipeMessage", json);

        var writer = new StringWriter();
        var mapper = JacksonMapper.newMapper();
        try (JsonGenerator g = mapper.createGenerator(writer).setPrettyPrinter(new DefaultPrettyPrinter())) {
            g.writeTree(msg);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't serialize request", ex);
        }

        var content = writer.toString();
        TrackEvent.trace("beacon", "Sending raw response:\n" + content);
        try (OutputStream blockOut = BeaconFormat.writeBlocks(outSocket.getOutputStream())) {
            blockOut.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void sendClientErrorResponse(Socket outSocket, String message) throws Exception {
        var err = new ClientErrorMessage(message);
        ObjectNode json = JacksonMapper.newMapper().valueToTree(err);
        var msg = JsonNodeFactory.instance.objectNode();
        msg.set("xPipeClientError", json);

        // Don't log this as it clutters the output
        // TrackEvent.trace("beacon", "Sending raw client error:\n" + json.toPrettyString());

        var mapper = JacksonMapper.newMapper();
        try (OutputStream blockOut = BeaconFormat.writeBlocks(outSocket.getOutputStream())) {
            var gen = mapper.createGenerator(blockOut);
            gen.writeTree(msg);
        }
    }

    public void sendServerErrorResponse(Socket outSocket, Throwable ex) throws Exception {
        var err = new ServerErrorMessage(UUID.randomUUID(), ex);
        ObjectNode json = JacksonMapper.newMapper().valueToTree(err);
        var msg = JsonNodeFactory.instance.objectNode();
        msg.set("xPipeServerError", json);

        // Don't log this as it clutters the output
        // TrackEvent.trace("beacon", "Sending raw server error:\n" + json.toPrettyString());

        var mapper = JacksonMapper.newMapper();
        try (OutputStream blockOut = BeaconFormat.writeBlocks(outSocket.getOutputStream())) {
            var gen = mapper.createGenerator(blockOut);
            gen.writeTree(msg);
        }
    }

    private <T extends RequestMessage> T parseRequest(JsonNode header) throws Exception {
        ObjectNode content = (ObjectNode) header.required("xPipeMessage");
        TrackEvent.trace("beacon", "Parsed raw request:\n" + content.toPrettyString());

        var type = content.required("messageType").textValue();
        var phase = content.required("messagePhase").textValue();
        if (!phase.equals("request")) {
            throw new IllegalArgumentException("Not a request");
        }
        content.remove("messageType");
        content.remove("messagePhase");

        var prov = MessageExchangeImpls.byId(type);
        if (prov.isEmpty()) {
            throw new IllegalArgumentException("Unknown request id: " + type);
        }

        var reader = JacksonMapper.newMapper().readerFor(prov.get().getRequestClass());
        return reader.readValue(content);
    }
}
