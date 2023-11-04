package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.core.store.DataStoreId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class LaunchExchange implements MessageExchange {

    @Override
    public String getId() {
        return "launch";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull DataStoreId id;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        @NonNull List<String> command;
    }
}
