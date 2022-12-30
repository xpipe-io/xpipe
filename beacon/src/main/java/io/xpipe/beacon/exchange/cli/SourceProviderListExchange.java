package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.exchange.data.ProviderEntry;
import io.xpipe.core.source.DataSourceType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

public class SourceProviderListExchange implements MessageExchange {

    @Override
    public String getId() {
        return "sourceProviderList";
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
        @NonNull
        Map<DataSourceType, List<ProviderEntry>> entries;
    }
}
