package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.store.DataStore;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ReadPreparationExchange implements MessageExchange<ReadPreparationExchange.Request, ReadPreparationExchange.Response> {

    @Override
    public String getId() {
        return "readPreparation";
    }

    @Override
    public Class<ReadPreparationExchange.Request> getRequestClass() {
        return ReadPreparationExchange.Request.class;
    }

    @Override
    public Class<ReadPreparationExchange.Response> getResponseClass() {
        return ReadPreparationExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        String providerType;
        String dataStore;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        DataSourceConfigInstance config;
        DataStore dataStore;
    }
}
