package io.xpipe.beacon.exchange;

import io.xpipe.beacon.NamedFunction;
import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class NamedFunctionExchange implements MessageExchange {

    @Override
    public String getId() {
        return "namedFunction";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        NamedFunction<?> function;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {

        Object returnValue;
    }
}
