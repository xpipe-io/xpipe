package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceReference;
import lombok.Builder;
import lombok.NonNull;
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
        @NonNull
        DataSourceReference ref;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
    }
}
