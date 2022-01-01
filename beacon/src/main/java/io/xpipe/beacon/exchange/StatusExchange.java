package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class StatusExchange implements MessageExchange<StatusExchange.Request, StatusExchange.Response> {

    @Override
    public String getId() {
        return "status";
    }

    @Override
    public Class<Request> getRequestClass() {
        return Request.class;
    }

    @Override
    public Class<Response> getResponseClass() {
        return Response.class;
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
        String mode;
    }
}
