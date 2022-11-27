package io.xpipe.beacon.exchange;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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

        @JsonSerialize(using = NamedFunction.Serializer.class, as = NamedFunction.class)
        @JsonDeserialize(using = NamedFunction.Deserializer.class, as = NamedFunction.class)
        NamedFunction<?> function;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {

        Object returnValue;
    }
}
