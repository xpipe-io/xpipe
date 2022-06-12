package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Queries general information about a data source.
 */
public class QueryDataSourceExchange implements MessageExchange {

    @Override
    public String getId() {
        return "queryDataSource";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSourceReference ref;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        @NonNull
        DataSourceId id;
        @NonNull
        DataSourceInfo info;
        @NonNull
        DataStore store;
        @NonNull
        DataSourceConfigInstance config;
    }
}
