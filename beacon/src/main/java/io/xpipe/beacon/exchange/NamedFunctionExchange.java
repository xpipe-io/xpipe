package io.xpipe.beacon.exchange;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import lombok.Builder;
import lombok.NonNull;
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
        @NonNull
        String id;

        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="type")
        @NonNull Object[] arguments;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {

        Object returnValue;
    }
}
