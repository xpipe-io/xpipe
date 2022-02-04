package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class EditExecuteExchange implements MessageExchange<EditExecuteExchange.Request, EditExecuteExchange.Response> {

    @Override
    public String getId() {
        return "editExecute";
    }

    @Override
    public Class<EditExecuteExchange.Request> getRequestClass() {
        return EditExecuteExchange.Request.class;
    }

    @Override
    public Class<EditExecuteExchange.Response> getResponseClass() {
        return EditExecuteExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull DataSourceReference ref;
        @NonNull
        DataSourceConfigInstance config;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
    }
}
