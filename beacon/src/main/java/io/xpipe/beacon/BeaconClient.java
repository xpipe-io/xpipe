package io.xpipe.beacon;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.xpipe.beacon.exchange.MessageExchanges;
import io.xpipe.beacon.exchange.data.ClientErrorMessage;
import io.xpipe.beacon.exchange.data.ServerErrorMessage;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.Deobfuscator;
import io.xpipe.core.util.JacksonMapper;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static io.xpipe.beacon.BeaconConfig.BODY_SEPARATOR;

public class BeaconClient implements AutoCloseable {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public abstract static class ClientInformation {

        public final CliClientInformation cli() {
            return (CliClientInformation) this;
        }

        public abstract String toDisplayString();
    }

    @JsonTypeName("cli")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class CliClientInformation extends ClientInformation {

        int consoleWidth;

        @Override
        public String toDisplayString() {
            return "X-Pipe CLI";
        }
    }

    @JsonTypeName("reachableCheck")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class ReachableCheckInformation extends ClientInformation {

        @Override
        public String toDisplayString() {
            return "Reachable check";
        }
    }

    @JsonTypeName("gateway")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class GatewayClientInformation extends ClientInformation {

        String version;

        @Override
        public String toDisplayString() {
            return "X-Pipe Gateway " + version;
        }
    }

    @JsonTypeName("api")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class ApiClientInformation extends ClientInformation {

        String version;
        String language;

        @Override
        public String toDisplayString() {
            return String.format("X-Pipe %s API v%s", language, version);
        }
    }

    @Getter
    private final Closeable base;

    private final InputStream in;
    private final OutputStream out;

    private BeaconClient(Closeable base, InputStream in, OutputStream out) {
        this.base = base;
        this.in = in;
        this.out = out;
    }

    public static BeaconClient connect(ClientInformation information) throws Exception {
        var socket = new Socket(InetAddress.getLoopbackAddress(), BeaconConfig.getUsedPort());
        var client = new BeaconClient(socket, socket.getInputStream(), socket.getOutputStream());
        client.sendObject(JacksonMapper.newMapper().valueToTree(information));
        return client;
    }

    public static BeaconClient connectProxy(ShellStore proxy) throws Exception {
        var control = proxy.create().start();
        var command = control.command("xpipe beacon --raw").start();
        command.discardErr();
        return new BeaconClient(command, command.getStdout(), command.getStdin()) {

//            {
//                new Thread(() -> {
//                    while (true) {
//                        if (!control.isRunning()) {
//                            close();
//                        }
//                    }
//                })
//            }

            @Override
            public <T extends ResponseMessage> T receiveResponse()
                    throws ConnectorException, ClientException, ServerException {
                try {
                    sendEOF();
                    getRawOutputStream().close();
                } catch (IOException ex) {
                    throw new ConnectorException(ex);
                }

                return super.receiveResponse();
            }

            @Override
            public void close() throws ConnectorException {
                try {
                    getRawInputStream().readAllBytes();
                } catch (IOException ex) {
                    throw new ConnectorException(ex);
                }

                super.close();
            }
        };
    }

    public static Optional<BeaconClient> tryConnect(ClientInformation information) {
        try {
            return Optional.of(connect(information));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public void close() throws ConnectorException {
        try {
            base.close();
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't close client", ex);
        }
    }

    public InputStream receiveBody() throws ConnectorException {
        try {
            var sep = in.readNBytes(BODY_SEPARATOR.length);
            if (sep.length != 0 && !Arrays.equals(BODY_SEPARATOR, sep)) {
                throw new ConnectorException("Invalid body separator");
            }
            return BeaconFormat.readBlocks(in);
        } catch (IOException ex) {
            throw new ConnectorException(ex);
        }
    }

    public OutputStream sendBody() throws ConnectorException {
        try {
            out.write(BODY_SEPARATOR);
            return BeaconFormat.writeBlocks(out);
        } catch (IOException ex) {
            throw new ConnectorException(ex);
        }
    }

    public <T extends RequestMessage> void sendRequest(T req) throws ClientException, ConnectorException {
        ObjectNode json = JacksonMapper.getDefault().valueToTree(req);
        var prov = MessageExchanges.byRequest(req);
        if (prov.isEmpty()) {
            throw new ClientException("Unknown request class " + req.getClass());
        }

        json.set("messageType", new TextNode(prov.get().getId()));
        json.set("messagePhase", new TextNode("request"));
        // json.set("id", new TextNode(UUID.randomUUID().toString()));
        var msg = JsonNodeFactory.instance.objectNode();
        msg.set("xPipeMessage", json);

        if (BeaconConfig.printMessages()) {
            System.out.println(
                    "Sending request to server of type " + req.getClass().getName());
        }

        sendObject(msg);
    }

    public void sendEOF() throws ConnectorException {
        try (OutputStream ignored = BeaconFormat.writeBlocks(out)) {
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't write to socket", ex);
        }
    }

    public void sendObject(JsonNode node) throws ConnectorException {
        var writer = new StringWriter();
        var mapper = JacksonMapper.newMapper();
        try (JsonGenerator g = mapper.createGenerator(writer).setPrettyPrinter(new DefaultPrettyPrinter())) {
            g.writeTree(node);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't serialize request", ex);
        }

        var content = writer.toString();
        if (BeaconConfig.printMessages()) {
            System.out.println("Sending raw request:");
            System.out.println(content);
        }

        try (OutputStream blockOut = BeaconFormat.writeBlocks(out)) {
            blockOut.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't write to socket", ex);
        }
    }

    public <T extends ResponseMessage> T receiveResponse() throws ConnectorException, ClientException, ServerException {
        JsonNode node;
        try (InputStream blockIn = BeaconFormat.readBlocks(in)) {
            node = JacksonMapper.newMapper().readTree(blockIn);
        } catch (SocketException ex) {
            throw new ConnectorException("Connection to xpipe daemon closed unexpectedly", ex);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't read from socket", ex);
        }

        if (BeaconConfig.printMessages()) {
            System.out.println("Received response:");
            System.out.println(node.toPrettyString());
        }

        if (node.isMissingNode()) {
            throw new ConnectorException("Received unexpected EOF");
        }

        var se = parseServerError(node);
        if (se.isPresent()) {
            se.get().throwError();
        }

        var ce = parseClientError(node);
        if (ce.isPresent()) {
            throw ce.get().throwException();
        }

        return parseResponse(node);
    }

    private Optional<ClientErrorMessage> parseClientError(JsonNode node) throws ConnectorException {
        ObjectNode content = (ObjectNode) node.get("xPipeClientError");
        if (content == null) {
            return Optional.empty();
        }

        try {
            var message = JacksonMapper.getDefault().treeToValue(content, ClientErrorMessage.class);
            return Optional.of(message);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't parse client error message", ex);
        }
    }

    private Optional<ServerErrorMessage> parseServerError(JsonNode node) throws ConnectorException {
        ObjectNode content = (ObjectNode) node.get("xPipeServerError");
        if (content == null) {
            return Optional.empty();
        }

        try {
            var message = JacksonMapper.getDefault().treeToValue(content, ServerErrorMessage.class);
            Deobfuscator.deobfuscate(message.getError());
            return Optional.of(message);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't parse server error message", ex);
        }
    }

    private <T extends ResponseMessage> T parseResponse(JsonNode header) throws ConnectorException {
        ObjectNode content = (ObjectNode) header.required("xPipeMessage");

        var type = content.required("messageType").textValue();
        var phase = content.required("messagePhase").textValue();
        // var requestId = UUID.fromString(content.required("id").textValue());
        if (!phase.equals("response")) {
            throw new IllegalArgumentException();
        }
        content.remove("messageType");
        content.remove("messagePhase");
        // content.remove("id");

        var prov = MessageExchanges.byId(type);
        if (prov.isEmpty()) {
            throw new IllegalArgumentException("Unknown response id " + type);
        }

        try {
            var reader = JacksonMapper.newMapper().readerFor(prov.get().getResponseClass());
            return reader.readValue(content);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't parse response", ex);
        }
    }

    public InputStream getRawInputStream() {
        return in;
    }

    public OutputStream getRawOutputStream() {
        return out;
    }

    @FunctionalInterface
    public interface FailableBiConsumer<T, U, E extends Throwable> {

        void accept(T var1, U var2) throws E;
    }

    @FunctionalInterface
    public interface FailableConsumer<T, E extends Throwable> {

        void accept(T var1) throws E;
    }

    @FunctionalInterface
    public interface FailableRunnable<E extends Throwable> {

        void run() throws E;
    }
}
