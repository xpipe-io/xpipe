package io.xpipe.beacon.message.impl;

import io.xpipe.app.core.OperationMode;
import io.xpipe.beacon.socket.SocketServer;
import io.xpipe.beacon.message.MessageProvider;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

import java.io.InputStream;
import java.net.Socket;
import java.util.stream.Collectors;

public class ModeExchange implements MessageProvider<ModeExchange.Request, ModeExchange.Response> {

    @Override
    public String getId() {
        return "mode";
    }

    @Override
    public Class<ModeExchange.Request> getRequestClass() {
        return ModeExchange.Request.class;
    }

    @Override
    public Class<ModeExchange.Response> getResponseClass() {
        return ModeExchange.Response.class;
    }

    public static record Request(String modeId) implements RequestMessage {

    }

    public static record Response() implements ResponseMessage {

    }
}
