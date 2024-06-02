package io.xpipe.app.beacon.impl;


import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconSession;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.HandshakeExchange;

import java.io.IOException;
import java.util.UUID;

public class HandshakeExchangeImpl extends HandshakeExchange {

    @Override
    public Object handle(HttpExchange exchange, Request body) throws IOException, BeaconClientException, BeaconServerException {
        var session = new BeaconSession(body.getClient(), UUID.randomUUID().toString());
        AppBeaconServer.get().addSession(session);
        return Response.builder().token(session.getToken()).build();
    }
}
