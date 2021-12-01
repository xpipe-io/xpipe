package io.xpipe.beacon.message.impl;

import io.xpipe.app.core.OperationMode;
import io.xpipe.beacon.socket.SocketServer;
import io.xpipe.beacon.message.MessageProvider;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

import java.io.InputStream;
import java.net.Socket;

public class StatusExchange implements MessageProvider<StatusExchange.Request, StatusExchange.Response> {

    @Override
    public String getId() {
        return "status";
    }

    @Override
    public Class<Request> getRequestClass() {
        return Request.class;
    }

    @Override
    public Class<Response> getResponseClass() {
        return Response.class;
    }

    public static record Request() implements RequestMessage {

    }

    public static record Response(String mode) implements ResponseMessage {

    }
}
