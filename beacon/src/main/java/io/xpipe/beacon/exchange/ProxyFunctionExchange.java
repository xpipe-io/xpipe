package io.xpipe.beacon.exchange;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.core.util.ProxyFunction;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ProxyFunctionExchange implements MessageExchange {

    @Override
    public String getId() {
        return "proxyFunction";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {

        @JsonSerialize(
                using = ProxyFunction.Serializer.class,
                as = ProxyFunction.class
        )
        @JsonDeserialize(
                using = ProxyFunction.Deserializer.class,
                as = ProxyFunction.class
        )
        ProxyFunction function;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {

        @JsonSerialize(
                using = ProxyFunction.Serializer.class,
                as = ProxyFunction.class
        )
        @JsonDeserialize(
                using = ProxyFunction.Deserializer.class,
                as = ProxyFunction.class
        )
        ProxyFunction function;
    }
}
