package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.store.DataStore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Prepares a client to send stream-based data to a daemon.
 */
public class ReadPreparationExchange implements MessageExchange {

    @Override
    public String getId() {
        return "readPreparation";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        String provider;

        @NonNull
        DataStore store;

        boolean configureAll;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        String determinedType;

        @NonNull
        DataSourceConfigInstance config;
    }
}
