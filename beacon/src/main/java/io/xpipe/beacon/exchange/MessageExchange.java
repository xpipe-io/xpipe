package io.xpipe.beacon.exchange;

import lombok.SneakyThrows;

/**
 * A message exchange scheme that implements a certain functionality.
 */
public interface MessageExchange {

    /**
     * The unique id of this exchange that will be included in the messages.
     */
    String getId();

    /**
     * Returns the request class, needed for serialization.
     */
    @SneakyThrows
    default Class<?> getRequestClass() {
        var c = getClass().getSuperclass();
        var name = (MessageExchange.class.isAssignableFrom(c) ? c : getClass()).getName() + "$Request";
        return Class.forName(name);
    }

    /**
     * Returns the response class, needed for serialization.
     */
    @SneakyThrows
    default Class<?> getResponseClass() {
        var c = getClass().getSuperclass();
        var name = (MessageExchange.class.isAssignableFrom(c) ? c : getClass()).getName() + "$Response";
        return Class.forName(name);
    }
}
