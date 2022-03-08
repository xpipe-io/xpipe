package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.store.DataStore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Sends stream-based data to a daemon.
 */
public class ReadExecuteExchange implements MessageExchange<ReadExecuteExchange.Request, ReadExecuteExchange.Response> {

    @Override
    public String getId() {
        return "readExecute";
    }

    @Override
    public Class<ReadExecuteExchange.Request> getRequestClass() {
        return ReadExecuteExchange.Request.class;
    }

    @Override
    public Class<ReadExecuteExchange.Response> getResponseClass() {
        return ReadExecuteExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataStore dataStore;
        @NonNull
        DataSourceConfigInstance config;

        DataSourceId target;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
    }
}
