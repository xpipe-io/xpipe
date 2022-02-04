package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.DataStore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class WritePreparationExchange implements MessageExchange<WritePreparationExchange.Request, WritePreparationExchange.Response> {

    @Override
    public String getId() {
        return "writePreparation";
    }

    @Override
    public Class<WritePreparationExchange.Request> getRequestClass() {
        return WritePreparationExchange.Request.class;
    }

    @Override
    public Class<WritePreparationExchange.Response> getResponseClass() {
        return WritePreparationExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        String providerType;
        String output;
        @NonNull
        DataSourceReference ref;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        DataStore dataStore;

        @NonNull
        DataSourceConfigInstance config;
    }
}
