package io.xpipe.beacon;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Optional;

import static io.xpipe.beacon.BeaconConfig.BODY_SEPARATOR;

public class BeaconClient {

    @FunctionalInterface
    public interface FailableBiConsumer<T, U, E extends Throwable> {

        void accept(T var1, U var2) throws E;
    }

    @FunctionalInterface
    public interface FailableConsumer<T, E extends Throwable> {

        void accept(T var1) throws E;
    }

    public static Optional<BeaconClient> tryConnect() {
        if (BeaconConfig.debugEnabled()) {
            System.out.println("Attempting connection to server at port " + BeaconConfig.getUsedPort());
        }

        try {
            return Optional.of(new BeaconClient());
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    public BeaconClient() throws IOException {
        socket = new Socket(InetAddress.getLoopbackAddress(), BeaconConfig.getUsedPort());
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    public void close() throws ConnectorException {
        try {
            socket.close();
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't close socket", ex);
        }
    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> void exchange(
            REQ req,
            FailableConsumer<OutputStream, IOException> reqWriter,
            FailableBiConsumer<RES, InputStream, IOException> resReader)
            throws ConnectorException, ClientException, ServerException {
        try {
            sendRequest(req);
            if (reqWriter != null) {
                out.write(BODY_SEPARATOR);
                reqWriter.accept(out);
            }

            var res = this.<RES>receiveResponse();
            var sep = in.readNBytes(BODY_SEPARATOR.length);
            if (sep.length != 0 && !Arrays.equals(BODY_SEPARATOR, sep)) {
                throw new ConnectorException("Invalid body separator");
            }

            resReader.accept(res, in);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't communicate with socket", ex);
        } finally {
            close();
        }
    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> RES simpleExchange(REQ req)
            throws ServerException, ConnectorException, ClientException {
        try {
            sendRequest(req);
            return this.receiveResponse();
        } finally {
            close();
        }
    }

    private <T extends RequestMessage> void sendRequest(T req) throws ClientException, ConnectorException {
        ObjectNode json = JacksonHelper.newMapper().valueToTree(req);
        var prov = MessageExchanges.byRequest(req);
        if (prov.isEmpty()) {
            throw new ClientException("Unknown request class " + req.getClass());
        }

        json.set("type", new TextNode(prov.get().getId()));
        json.set("phase", new TextNode("request"));
        //json.set("id", new TextNode(UUID.randomUUID().toString()));
        var msg = JsonNodeFactory.instance.objectNode();
        msg.set("xPipeMessage", json);


        try {
            var mapper = JacksonHelper.newMapper().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            var gen = mapper.createGenerator(socket.getOutputStream());
            gen.writeTree(msg);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't write to socket", ex);
        }
    }

    private <T extends ResponseMessage> T receiveResponse() throws ConnectorException, ClientException, ServerException {
        JsonNode read;
        try {
            var in = socket.getInputStream();
            read = JacksonHelper.newMapper().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE).readTree(in);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't read from socket", ex);
        }

        if (BeaconConfig.debugEnabled()) {
            System.out.println("Recieved response:");
            System.out.println(read.toPrettyString());
        }

        var se = parseServerError(read);
        if (se.isPresent()) {
            se.get().throwError();
        }

        var ce = parseClientError(read);
        if (ce.isPresent()) {
            throw ce.get().throwException();
        }

        return parseResponse(read);
    }

    private Optional<ClientErrorMessage> parseClientError(JsonNode node) throws ConnectorException {
        ObjectNode content = (ObjectNode) node.get("xPipeClientError");
        if (content == null) {
            return Optional.empty();
        }

        try {
            var reader = JacksonHelper.newMapper().readerFor(ClientErrorMessage.class);
            return Optional.of(reader.readValue(content));
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
            var reader = JacksonHelper.newMapper().readerFor(ServerErrorMessage.class);
            return Optional.of(reader.readValue(content));
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't parse server error message", ex);
        }
    }

    private <T extends ResponseMessage> T parseResponse(JsonNode header) throws ConnectorException {
        ObjectNode content = (ObjectNode) header.required("xPipeMessage");

        var type = content.required("type").textValue();
        var phase = content.required("phase").textValue();
        //var requestId = UUID.fromString(content.required("id").textValue());
        if (!phase.equals("response")) {
            throw new IllegalArgumentException();
        }
        content.remove("type");
        content.remove("phase");
        //content.remove("id");

        var prov = MessageExchanges.byId(type);
        if (prov.isEmpty()) {
            throw new IllegalArgumentException("Unknown response id " + type);
        }

        try {
            var reader = JacksonHelper.newMapper().readerFor(prov.get().getResponseClass());
            return reader.readValue(content);
        } catch (IOException ex) {
            throw new ConnectorException("Couldn't parse response", ex);
        }
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }
}
