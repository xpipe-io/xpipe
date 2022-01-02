package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfig;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.net.URL;

public class StoreEditExchange implements MessageExchange<StoreEditExchange.Request, StoreEditExchange.Response> {

    @Override
    public String getId() {
        return "storeEdit";
    }

    @Override
    public Class<StoreEditExchange.Request> getRequestClass() {
        return StoreEditExchange.Request.class;
    }

    @Override
    public Class<StoreEditExchange.Response> getResponseClass() {
        return StoreEditExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        DataSourceId sourceId;
        DataSourceConfig config;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
    }
}
