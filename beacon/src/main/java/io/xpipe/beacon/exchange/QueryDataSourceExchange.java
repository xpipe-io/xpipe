package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class QueryDataSourceExchange implements MessageExchange<QueryDataSourceExchange.Request, QueryDataSourceExchange.Response> {

    @Override
    public String getId() {
        return "queryDataSource";
    }

    @Override
    public Class<QueryDataSourceExchange.Request> getRequestClass() {
        return QueryDataSourceExchange.Request.class;
    }

    @Override
    public Class<QueryDataSourceExchange.Response> getResponseClass() {
        return QueryDataSourceExchange.Response.class;
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
