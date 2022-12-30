package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.core.store.DataStore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;

/**
 * Queries general information about a data source.
 */
public class QueryStoreExchange implements MessageExchange {

    @Override
    public String getId() {
        return "queryStore";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        String name;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        @NonNull
        String name;

        String information;

        String summary;

        @NonNull
        String provider;

        @NonNull
        LinkedHashMap<String, String> config;

        DataStore internalStore;
    }
}
