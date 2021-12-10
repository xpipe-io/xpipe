package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.beacon.socket.SocketServer;

import java.io.InputStream;
import java.net.Socket;

public interface MessageExchange<RQ extends RequestMessage, RP extends ResponseMessage> {

    String getId();

    Class<RQ> getRequestClass();

    Class<RP> getResponseClass();

    void handleRequest(SocketServer server, RQ msg, InputStream body, Socket clientSocket) throws Exception;
}
