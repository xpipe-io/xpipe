package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Requests the daemon to stop.
 */
public class StopExchange implements MessageExchange {

    @Override
    public String getId() {
        return "stop";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {}

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        boolean success;
    }
}
