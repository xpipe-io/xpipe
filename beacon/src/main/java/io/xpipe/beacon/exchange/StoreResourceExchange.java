package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.DataSourceConfig;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.net.URL;

public class StoreResourceExchange implements MessageExchange<StoreResourceExchange.Request, StoreResourceExchange.Response> {

    @Override
    public String getId() {
        return "storeResource";
    }

    @Override
    public Class<StoreResourceExchange.Request> getRequestClass() {
        return StoreResourceExchange.Request.class;
    }

    @Override
    public Class<StoreResourceExchange.Response> getResponseClass() {
        return StoreResourceExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        URL url;
        String providerId;
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
