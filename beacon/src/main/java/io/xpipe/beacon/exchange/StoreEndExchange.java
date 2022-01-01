package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceId;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;
import java.util.UUID;

public class StoreEndExchange implements MessageExchange<StoreEndExchange.Request, StoreEndExchange.Response> {

    @Override
    public String getId() {
        return "storeEnd";
    }

    @Override
    public Class<StoreEndExchange.Request> getRequestClass() {
        return StoreEndExchange.Request.class;
    }

    @Override
    public Class<StoreEndExchange.Response> getResponseClass() {
        return StoreEndExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        UUID entryId;
        Map<String, String> values;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        DataSourceId sourceId;
    }
}
