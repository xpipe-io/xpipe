package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.extension.cli.CliOptionPage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class StoreStartExchange implements MessageExchange<StoreStartExchange.Request, StoreStartExchange.Response> {

    @Override
    public String getId() {
        return "storeStart";
    }

    @Override
    public Class<StoreStartExchange.Request> getRequestClass() {
        return StoreStartExchange.Request.class;
    }

    @Override
    public Class<StoreStartExchange.Response> getResponseClass() {
        return StoreStartExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        String type;
        boolean hasInputStream;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        CliOptionPage page;
    }
}
