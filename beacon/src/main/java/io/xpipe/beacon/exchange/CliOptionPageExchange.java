package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.source.DataSourceId;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class CliOptionPageExchange implements MessageExchange<CliOptionPageExchange.Request, CliOptionPageExchange.Response> {

    @Override
    public String getId() {
        return "cliOptionPage";
    }

    @Override
    public Class<CliOptionPageExchange.Request> getRequestClass() {
        return CliOptionPageExchange.Request.class;
    }

    @Override
    public Class<CliOptionPageExchange.Response> getResponseClass() {
        return CliOptionPageExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        DataSourceId newSourceId;
        String type;
        boolean hasInputStream;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        DataSourceId sourceId;
        DataType dataType;
        int rowCount;
    }
}
