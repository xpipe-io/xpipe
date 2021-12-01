package io.xpipe.beacon.message.impl;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.beacon.socket.SocketServer;
import io.xpipe.beacon.message.MessageProvider;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

import java.io.InputStream;
import java.net.Socket;

public class VersionExchange implements MessageProvider<VersionExchange.Request, VersionExchange.Response> {

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