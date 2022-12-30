package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class RenameEntryExchange implements MessageExchange {

    @Override
    public String getId() {
        return "renameEntry";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSourceReference ref;

        @NonNull
        DataSourceId newId;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
    }
}
