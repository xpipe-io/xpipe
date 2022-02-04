package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceInfo;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.DataStore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class InfoExchange implements MessageExchange<InfoExchange.Request, InfoExchange.Response> {

    @Override
    public String getId() {
        return "info";
    }

    @Override
    public Class<InfoExchange.Request> getRequestClass() {
        return InfoExchange.Request.class;
    }

    @Override
    public Class<InfoExchange.Response> getResponseClass() {
        return InfoExchange.Response.class;
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
        DataSourceInfo info;
        @NonNull
        DataStore store;
        @NonNull
        DataSourceConfigInstance config;
    }
}
