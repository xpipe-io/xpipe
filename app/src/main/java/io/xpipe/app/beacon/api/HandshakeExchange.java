package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.*;
import io.xpipe.app.beacon.BeaconAuthMethod;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class HandshakeExchange extends BeaconInterface<HandshakeExchange.Request> {

    @Override
    public boolean acceptInShutdown() {
        return true;
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }

    @Override
    public String getPath() {
        return "/handshake";
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request request) throws BeaconClientException {
        if (!checkAuth(request.getAuth())) {
            throw new BeaconClientException("Authentication failed");
        }

        TrackEvent.withTrace("Handshake request received")
                .tag("client", request.getClient().toDisplayString())
                .handle();

        var session = new BeaconSession(request.getClient(), UUID.randomUUID().toString());
        AppBeaconServer.get().addSession(session);
        return Response.builder().sessionToken(session.getToken()).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    private boolean checkAuth(io.xpipe.app.beacon.BeaconAuthMethod authMethod) {
        if (authMethod instanceof BeaconAuthMethod.Local local) {
            var c = local.getAuthFileContent().strip();
            return AppBeaconServer.get().getLocalAuthSecret().equals(c);
        }

        if (authMethod instanceof BeaconAuthMethod.ApiKey key) {
            var c = key.getKey().strip();
            return AppPrefs.get().apiKey().get().equals(c);
        }

        return false;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        BeaconAuthMethod auth;

        @NonNull
        BeaconClientInformation client;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        String sessionToken;
    }
}
