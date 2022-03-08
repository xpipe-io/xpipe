package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class VersionExchange implements MessageExchange<VersionExchange.Request, VersionExchange.Response> {

    @Override
    public String getId() {
        return "version";
    }

    @Override
    public Class<VersionExchange.Request> getRequestClass() {
        return VersionExchange.Request.class;
    }

    @Override
    public Class<VersionExchange.Response> getResponseClass() {
        return VersionExchange.Response.class;
    }

    @lombok.extern.jackson.Jacksonized
    @lombok.Builder
    @lombok.Value
    public static class Request implements RequestMessage {

    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {

        String version;
        String buildVersion;
        String jvmVersion;
    }
}