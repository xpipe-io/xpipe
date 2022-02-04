package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class EditPreparationExchange implements MessageExchange<EditPreparationExchange.Request, EditPreparationExchange.Response> {

    @Override
    public String getId() {
        return "editPreparation";
    }

    @Override
    public Class<EditPreparationExchange.Request> getRequestClass() {
        return EditPreparationExchange.Request.class;
    }

    @Override
    public Class<EditPreparationExchange.Response> getResponseClass() {
        return EditPreparationExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSourceReference ref;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        DataSourceConfigInstance config;
    }
}
