package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

public abstract class VersionExchange implements MessageExchange<VersionExchange.Request, VersionExchange.Response> {

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

    public static record Request() implements RequestMessage {

    }

    public static class Response implements ResponseMessage {

        public final String version;
        public final String jvmVersion;

        public Response(String version, String jvmVersion) {
            this.version = version;
            this.jvmVersion = jvmVersion;
        }
    }
}