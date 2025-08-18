package io.xpipe.beacon;

import io.xpipe.beacon.api.HandshakeExchange;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.XPipeInstallation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Optional;

public class BeaconClient {

    private final int port;
    private String token;

    public BeaconClient(int port) {
        this.port = port;
    }

    public static BeaconClient establishConnection(int port, BeaconClientInformation information) throws Exception {
        var client = new BeaconClient(port);
        var auth = Files.readString(XPipeInstallation.getLocalBeaconAuthFile());
        HandshakeExchange.Response response = client.performRequest(HandshakeExchange.Request.builder()
                .client(information)
                .auth(BeaconAuthMethod.Local.builder().authFileContent(auth).build())
                .build());
        client.token = response.getSessionToken();
        return client;
    }

    public static Optional<BeaconClient> tryEstablishConnection(int port, BeaconClientInformation information) {
        try {
            return Optional.of(establishConnection(port, information));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public <RES> RES performRequest(BeaconInterface<?> prov, String rawNode)
            throws BeaconConnectorException, BeaconClientException, BeaconServerException {
        var content = rawNode;
        if (BeaconConfig.printMessages()) {
            System.out.println("Sending raw request:");
            System.out.println(content);
        }

        var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpResponse<String> response;
        try {
            // Use direct IP to prevent DNS lookups and potential blocks (e.g. portmaster)
            var uri = URI.create("http://127.0.0.1:" + port + prov.getPath());
            var builder = HttpRequest.newBuilder();
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            var httpRequest = builder.uri(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(content))
                    .build();
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new BeaconConnectorException("Couldn't send request", ex);
        }

        if (BeaconConfig.printMessages()) {
            System.out.println("Received raw response:");
            System.out.println(response.body());
        }

        var se = parseServerError(response);
        if (se.isPresent()) {
            se.get().throwError();
        }

        var ce = parseClientError(response);
        if (ce.isPresent()) {
            throw ce.get().throwException();
        }

        try {
            var reader = JacksonMapper.getDefault().readerFor(prov.getResponseClass());
            var emptyResponseClass = prov.getResponseClass().getDeclaredFields().length == 0;
            var body = response.body();
            if (emptyResponseClass && body.isBlank()) {
                return createDefaultResponse(prov);
            }
            var v = (RES) reader.readValue(body);
            return v;
        } catch (IOException ex) {
            throw new BeaconConnectorException("Couldn't parse response", ex);
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private <REQ> REQ createDefaultResponse(BeaconInterface<?> beaconInterface) {
        var c = beaconInterface.getResponseClass().getDeclaredMethod("builder");
        c.setAccessible(true);
        var b = c.invoke(null);
        var m = b.getClass().getDeclaredMethod("build");
        m.setAccessible(true);
        return (REQ) beaconInterface.getResponseClass().cast(m.invoke(b));
    }

    public <REQ, RES> RES performRequest(REQ req)
            throws BeaconConnectorException, BeaconClientException, BeaconServerException {
        ObjectNode node = JacksonMapper.getDefault().valueToTree(req);
        var prov = BeaconInterface.byRequest(req);
        if (prov.isEmpty()) {
            throw new IllegalArgumentException("Unknown request class " + req.getClass());
        }
        if (BeaconConfig.printMessages()) {
            System.out.println(
                    "Sending request to server of type " + req.getClass().getName());
        }

        return performRequest(prov.get(), node.toPrettyString());
    }

    private Optional<BeaconClientErrorResponse> parseClientError(HttpResponse<String> response)
            throws BeaconConnectorException {
        if (response.statusCode() < 400 || response.statusCode() > 499) {
            return Optional.empty();
        }

        try {
            var v = JacksonMapper.getDefault().readValue(response.body(), BeaconClientErrorResponse.class);
            return Optional.of(v);
        } catch (IOException ex) {
            throw new BeaconConnectorException("Couldn't parse client error message", ex);
        }
    }

    private Optional<BeaconServerErrorResponse> parseServerError(HttpResponse<String> response)
            throws BeaconConnectorException {
        if (response.statusCode() < 500 || response.statusCode() > 599) {
            return Optional.empty();
        }

        try {
            var v = JacksonMapper.getDefault().readValue(response.body(), BeaconServerErrorResponse.class);
            return Optional.of(v);
        } catch (IOException ex) {
            throw new BeaconConnectorException("Couldn't parse client error message", ex);
        }
    }
}
