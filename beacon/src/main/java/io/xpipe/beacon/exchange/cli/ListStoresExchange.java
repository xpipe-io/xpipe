package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.exchange.data.StoreListEntry;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class ListStoresExchange implements MessageExchange {

    @Override
    public String getId() {
        return "listStores";
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
        List<StoreListEntry> entries;
    }
}
