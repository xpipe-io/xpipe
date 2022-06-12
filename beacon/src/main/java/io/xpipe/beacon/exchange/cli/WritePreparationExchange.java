package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.StreamDataStore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Prepares a client to output the data source contents.
 */
public class WritePreparationExchange implements MessageExchange {

    @Override
    public String getId() {
        return "writePreparation";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        String type;
        @NonNull
        StreamDataStore output;
        @NonNull
        DataSourceReference ref;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        @NonNull
        DataSourceConfigInstance config;
    }
}
