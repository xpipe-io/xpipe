package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Output the data source contents.
 */
public class WriteExecuteExchange implements MessageExchange {

    @Override
    public String getId() {
        return "writeExecute";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSourceReference ref;

        @NonNull
        DataSourceConfigInstance config;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
    }
}
