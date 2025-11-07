package io.xpipe.app.beacon.impl;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconSession;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.beacon.BeaconAuthMethod;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.HandshakeExchange;

import com.sun.net.httpserver.HttpExchange;

import java.util.UUID;

public class HandshakeExchangeImpl extends HandshakeExchange {

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

    private boolean checkAuth(BeaconAuthMethod authMethod) {
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
}
