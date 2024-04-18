package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.exchange.data.CollectionListEntry;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class ListCollectionsExchange implements MessageExchange {

    @Override
    public String getId() {
        return "listCollections";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {}

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        List<CollectionListEntry> entries;
    }
}
