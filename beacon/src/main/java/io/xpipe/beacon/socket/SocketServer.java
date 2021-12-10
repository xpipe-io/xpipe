package io.xpipe.beacon.socket;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.xpipe.beacon.exchange.MessageExchanges;
import io.xpipe.beacon.message.ClientErrorMessage;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.beacon.message.ServerErrorMessage;
import io.xpipe.core.util.JacksonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class SocketServer {

    private static final String BEACON_PORT_PROP = "io.xpipe.beacon.port";
    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);

    private static final int DEFAULT_PORT = 21721;
    private static SocketServer INSTANCE;
    private final int port;
    private ServerSocket socket;
    private boolean running;
    private int connectionCounter;

    private SocketServer(int port) {
        this.port = port;
    }

    public static Path getUserDir() {
        return Path.of(System.getProperty("user.home"), ".xpipe");
    }

    public static int determineUsedPort() {
        if (System.getProperty(BEACON_PORT_PROP) != null) {
            return Integer.parseInt(System.getProperty(BEACON_PORT_PROP));
        }

        var file = getUserDir().resolve("port");
        if (Files.exists(file)) {
            try {
                return Integer.parseInt(Files.readString(file));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return DEFAULT_PORT;
    }

    public static void init() throws IOException {
        var port = determineUsedPort();
        INSTANCE = new SocketServer(port);
        INSTANCE.createSocket();
    }

    public static void reset() {
        INSTANCE.stop();
        INSTANCE = null;
    }

    private void stop() {

    }

    private void createSocket() throws IOException {
        socket = new ServerSocket(port, 1000, InetAddress.getLoopbackAddress());
        running = true;
        var t = new Thread(() -> {
            while (running) {
                try {
                    var clientSocket = socket.accept();
                    handleClientConnection(clientSocket);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                connectionCounter++;
            }
        }, "socket server");
        t.setDaemon(true);
        t.start();
    }

    private void handleClientConnection(Socket clientSocket) {
        var t = new Thread(() -> {
            try {
                var in = clientSocket.getInputStream();
                var read = JacksonHelper.newMapper().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE).readTree(in);
                logger.debug("Received request: \n" + read.toPrettyString());

                var req = parseRequest(read);
                var prov = MessageExchanges.byRequest(req).get();
                prov.handleRequest(this, req, in, clientSocket);
            } catch (SocketException ex) {
                try {
                    ex.printStackTrace();
                } catch (Exception ioex) {
                    ioex.printStackTrace();
                }
            } catch (Exception ex) {
                try {
                    ex.printStackTrace();
                    sendServerErrorResponse(clientSocket, ex);
                } catch (Exception ioex) {
                    ioex.printStackTrace();
                }
            } finally {
                try {
                    clientSocket.close();
                } catch (Exception ioex) {
                    ioex.printStackTrace();
                }
            }
        }, "socket connection #" + connectionCounter);
        t.setDaemon(true);
        t.start();
    }

    public void prepareBody(Socket outSocket) throws IOException {
        outSocket.getOutputStream().write(Sockets.BODY_SEPARATOR);
    }

    public <T extends ResponseMessage> void sendResponse(Socket outSocket, T obj) throws Exception {
        ObjectNode json = JacksonHelper.newMapper().valueToTree(obj);
        var prov = MessageExchanges.byResponse(obj).get();
        json.set("type", new TextNode(prov.getId()));
        json.set("phase", new TextNode("response"));
        var msg = JsonNodeFactory.instance.objectNode();
        msg.set("xPipeMessage", json);

        var mapper = JacksonHelper.newMapper().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        var gen = mapper.createGenerator(outSocket.getOutputStream());
        gen.writeTree(msg);
    }

    public void sendClientErrorResponse(Socket outSocket, String message) throws Exception {
        var err = new ClientErrorMessage(message);
        ObjectNode json = JacksonHelper.newMapper().valueToTree(err);
        var msg = JsonNodeFactory.instance.objectNode();
        msg.set("xPipeClientError", json);

        var mapper = JacksonHelper.newMapper().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        var gen = mapper.createGenerator(outSocket.getOutputStream());
        gen.writeTree(msg);
    }

    public void sendServerErrorResponse(Socket outSocket, Throwable ex) throws Exception {
        var err = new ServerErrorMessage(UUID.randomUUID(), ex);
        ObjectNode json = JacksonHelper.newMapper().valueToTree(err);
        var msg = JsonNodeFactory.instance.objectNode();
        msg.set("xPipeServerError", json);

        var mapper = JacksonHelper.newMapper().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        var gen = mapper.createGenerator(outSocket.getOutputStream());
        gen.writeTree(msg);
    }

    private <T extends RequestMessage> T parseRequest(JsonNode header) throws Exception {
        ObjectNode content = (ObjectNode) header.required("xPipeMessage");

        var type = content.required("type").textValue();
        var phase = content.required("phase").textValue();
        if (!phase.equals("request")) {
            throw new IllegalArgumentException();
        }
        content.remove("type");
        content.remove("phase");

        var prov = MessageExchanges.byId(type);
        if (prov.isEmpty()) {
            throw new IllegalArgumentException();
        }

        var reader = JacksonHelper.newMapper().readerFor(prov.get().getRequestClass());
        return reader.readValue(content);
    }
}
