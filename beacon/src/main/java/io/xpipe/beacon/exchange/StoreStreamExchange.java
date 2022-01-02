package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.DataSourceConfig;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class StoreStreamExchange implements MessageExchange<StoreStreamExchange.Request, StoreStreamExchange.Response> {

    @Override
    public String getId() {
        return "storeStream";
    }

    @Override
    public Class<StoreStreamExchange.Request> getRequestClass() {
        return StoreStreamExchange.Request.class;
    }

    @Override
    public Class<StoreStreamExchange.Response> getResponseClass() {
        return StoreStreamExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        String type;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        DataSourceId sourceId;
        DataSourceType sourceType;
        DataSourceConfig config;
        Object data;

        public ReadInfoExchange.TableData getTableData() {
            return (ReadInfoExchange.TableData) data;
        }
    }
}
