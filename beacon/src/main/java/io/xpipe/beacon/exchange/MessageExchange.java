package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

public interface MessageExchange<RQ extends RequestMessage, RP extends ResponseMessage> {

    String getId();

    Class<RQ> getRequestClass();

    Class<RP> getResponseClass();
}
