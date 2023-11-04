package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.exchange.data.ProviderEntry;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

public class StoreProviderListExchange implements MessageExchange {

    @Override
    public String getId() {
        return "storeProviderList";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {}

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        @NonNull Map<String, List<ProviderEntry>> entries;
    }
}
