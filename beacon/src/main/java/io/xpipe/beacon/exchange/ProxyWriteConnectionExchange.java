package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.WriteMode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ProxyWriteConnectionExchange implements MessageExchange {

    @Override
    public String getId() {
        return "proxyWriteConnection";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSource<?> source;

        @NonNull
        WriteMode mode;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {}
}
