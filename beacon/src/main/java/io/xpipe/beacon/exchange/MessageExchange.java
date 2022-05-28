package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import lombok.SneakyThrows;

/**
 * A message exchange scheme that implements a certain functionality.
 */
public interface MessageExchange<RQ extends RequestMessage, RP extends ResponseMessage> {

    /**
     * The unique id of this exchange that will be included in the messages.
     */
    String getId();

    /**
     * Returns the request class, needed for serialization.
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    default Class<RQ> getRequestClass() {
        var c = getClass().getSuperclass();
        var name = (MessageExchange.class.isAssignableFrom(c) ? c : getClass()).getName() + "$Request";
        return (Class<RQ>) Class.forName(name);
    }

    /**
     * Returns the response class, needed for serialization.
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    default Class<RP> getResponseClass() {
        var c = getClass().getSuperclass();
        var name = (MessageExchange.class.isAssignableFrom(c) ? c : getClass()).getName() + "$Response";
        return (Class<RP>) Class.forName(name);
    }
}
