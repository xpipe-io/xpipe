package io.xpipe.app.beacon.api;

import io.xpipe.app.beacon.*;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;

import com.sun.net.httpserver.HttpExchange;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
        if (AppProperties.get().isPrintBeaconMessages()) {
            TrackEvent.withTrace("Handshake request received").tag("client", request.getClient().toDisplayString()).handle();
        }

        if (!checkAuth(request.getAuth())) {
            throw new BeaconClientException("Authentication failed");
        }

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
            return checkEqualsConstant(AppBeaconServer.get().getLocalAuthSecret(), c);
        }

        if (authMethod instanceof BeaconAuthMethod.ApiKey key) {
            var c = key.getKey().strip();
            return checkEqualsConstant(AppPrefs.get().apiKey().get(), c);
        }

        return false;
    }

    private boolean checkEqualsConstant(String s1, String s2) {
        return MessageDigest.isEqual(s1.getBytes(StandardCharsets.UTF_8), s2.getBytes(StandardCharsets.UTF_8));
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
