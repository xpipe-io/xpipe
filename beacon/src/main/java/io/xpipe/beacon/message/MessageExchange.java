package io.xpipe.beacon.message;

import io.xpipe.beacon.socket.SocketServer;

import java.io.InputStream;
import java.net.Socket;

public interface MessageExchange<RQ extends RequestMessage, RP extends ResponseMessage> {

    String getId();

    Class<RQ> getRequestClass();

    Class<RP> getResponseClass();

    default void handleRequest(SocketServer server, RQ msg, InputStream body, Socket clientSocket) throws Exception {}
}
