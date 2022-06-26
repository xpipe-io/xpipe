package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.core.dialog.DialogReference;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.source.DataSourceType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ConvertExchange implements MessageExchange {

    @Override
    public String getId() {
        return "convert";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        DataSourceReference ref;

        String newProvider;

        DataSourceType newCategory;

        DataSourceId copyId;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        @NonNull
        DialogReference config;
    }
}
