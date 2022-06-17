package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.exchange.data.EntryListEntry;
import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class ListEntriesExchange implements MessageExchange {

    @Override
    public String getId() {
        return "listEntries";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        String collection;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        List<EntryListEntry> entries;
    }
}
