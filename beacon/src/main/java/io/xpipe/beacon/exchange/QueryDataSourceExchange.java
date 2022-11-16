package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.source.DataSourceType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;

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
        String information;

        @NonNull
        String provider;

        @NonNull DataSourceType type;

        @NonNull
        LinkedHashMap<String, String> config;

        DataSource<?> internalSource;
    }
}
