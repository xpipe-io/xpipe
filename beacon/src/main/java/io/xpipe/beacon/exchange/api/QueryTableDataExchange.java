package io.xpipe.beacon.exchange.api;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class QueryTableDataExchange implements MessageExchange<QueryTableDataExchange.Request, QueryTableDataExchange.Response> {

    @Override
    public String getId() {
        return "queryTableData";
    }

    @Override
    public Class<QueryTableDataExchange.Request> getRequestClass() {
        return QueryTableDataExchange.Request.class;
    }

    @Override
    public Class<QueryTableDataExchange.Response> getResponseClass() {
        return QueryTableDataExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSourceId id;

        @Builder.Default
        int maxRows = -1;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
    }
}
