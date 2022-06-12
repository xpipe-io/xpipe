package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Performs an edit for a data source.
 */
public class EditExecuteExchange implements MessageExchange {

    @Override
    public String getId() {
        return "editExecute";
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
        DataSourceId id;
    }
}
