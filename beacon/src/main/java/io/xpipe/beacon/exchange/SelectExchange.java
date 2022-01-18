package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceId;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class SelectExchange implements MessageExchange<SelectExchange.Request, SelectExchange.Response> {

    @Override
    public String getId() {
        return "select";
    }

    @Override
    public Class<SelectExchange.Request> getRequestClass() {
        return SelectExchange.Request.class;
    }

    @Override
    public Class<SelectExchange.Response> getResponseClass() {
        return SelectExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        DataSourceId id;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
    }
}
