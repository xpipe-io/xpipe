package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Requests the daemon to stop.
 */
public class StopExchange implements MessageExchange<StopExchange.Request, StopExchange.Response> {

    @Override
    public String getId() {
        return "stop";
    }

    @Override
    public Class<StopExchange.Request> getRequestClass() {
        return StopExchange.Request.class;
    }

    @Override
    public Class<StopExchange.Response> getResponseClass() {
        return StopExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        boolean success;
    }
}
