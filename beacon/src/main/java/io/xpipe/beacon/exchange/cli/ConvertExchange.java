package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ConvertExchange implements MessageExchange<ConvertExchange.Request, ConvertExchange.Response> {

    @Override
    public String getId() {
        return "convert";
    }

    @Override
    public Class<ConvertExchange.Request> getRequestClass() {
        return ConvertExchange.Request.class;
    }

    @Override
    public Class<ConvertExchange.Response> getResponseClass() {
        return ConvertExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSourceReference ref;

        @NonNull DataSourceId copyId;

        @NonNull
        DataSourceConfigInstance config;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
    }
}
