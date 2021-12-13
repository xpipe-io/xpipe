package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.beacon.BeaconHandler;

import java.io.InputStream;

public interface MessageExchange<RQ extends RequestMessage, RP extends ResponseMessage> {

    String getId();

    Class<RQ> getRequestClass();

    Class<RP> getResponseClass();

    void handleRequest(BeaconHandler handler, RQ msg, InputStream body) throws Exception;
}
