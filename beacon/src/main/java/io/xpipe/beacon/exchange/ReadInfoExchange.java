package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.source.DataSourceConfig;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ReadInfoExchange implements MessageExchange<ReadInfoExchange.Request, ReadInfoExchange.Response> {

    @Override
    public String getId() {
        return "readTableInfo";
    }

    @Override
    public Class<ReadInfoExchange.Request> getRequestClass() {
        return ReadInfoExchange.Request.class;
    }

    @Override
    public Class<ReadInfoExchange.Response> getResponseClass() {
        return ReadInfoExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        DataSourceId sourceId;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        DataSourceId sourceId;
        DataSourceType type;
        DataSourceConfig config;
        Object data;

        public TableData getTableData() {
            return (TableData) data;
        }
    }

    @Jacksonized
    @Builder
    @Value
    public static class TableData {
        DataType dataType;
        int rowCount;
    }
}
