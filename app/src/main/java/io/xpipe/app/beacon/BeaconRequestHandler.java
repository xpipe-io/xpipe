package io.xpipe.app.beacon;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.beacon.*;
import io.xpipe.core.util.JacksonMapper;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BeaconRequestHandler<T> implements HttpHandler {

    private final BeaconInterface<T> beaconInterface;

    public BeaconRequestHandler(BeaconInterface<T> beaconInterface) {this.beaconInterface = beaconInterface;}

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!AppPrefs.get().disableApiAuthentication().get() && beaconInterface.requiresAuthentication()) {
            var auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null) {
                writeError(exchange, new BeaconClientErrorResponse("Missing Authorization header"), 401);
                return;
            }

            var token = auth.replace("Bearer ", "");
            var session = AppBeaconServer.get().getSessions().stream().filter(s -> s.getToken().equals(token)).findFirst().orElse(null);
            if (session == null) {
                writeError(exchange, new BeaconClientErrorResponse("Unknown token"), 403);
                return;
            }
        }

        handleAuthenticatedRequest(exchange);
    }

    private void handleAuthenticatedRequest(HttpExchange exchange) {
        T object;
        Object response;
        try {
            try (InputStream is = exchange.getRequestBody()) {
                var tree = JacksonMapper.getDefault().readTree(is);
                TrackEvent.trace("Parsed raw request:\n" + tree.toPrettyString());
                var emptyRequestClass = tree.isEmpty() && beaconInterface.getRequestClass().getDeclaredFields().length == 0;
                object = emptyRequestClass ? createDefaultRequest(beaconInterface) : JacksonMapper.getDefault().treeToValue(tree, beaconInterface.getRequestClass());
                TrackEvent.trace("Parsed request object:\n" + object);
            }
            response = beaconInterface.handle(exchange, object);
        } catch (BeaconClientException clientException) {
            ErrorEvent.fromThrowable(clientException).omit().expected().handle();
            writeError(exchange, new BeaconClientErrorResponse(clientException.getMessage()), 400);
            return;
        } catch (BeaconServerException serverException) {
            var cause = serverException.getCause() != null ? serverException.getCause() : serverException;
            ErrorEvent.fromThrowable(cause).handle();
            writeError(exchange, new BeaconServerErrorResponse(cause), 500);
            return;
        } catch (IOException ex) {
            // Handle serialization errors as normal exceptions and other IO exceptions as assuming that the connection is broken
            if (!ex.getClass().getName().contains("jackson")) {
                ErrorEvent.fromThrowable(ex).omit().expected().handle();
            } else {
                ErrorEvent.fromThrowable(ex).omit().expected().handle();
                writeError(exchange, new BeaconClientErrorResponse(ex.getMessage()), 400);
            }
            return;
        } catch (Throwable other) {
            ErrorEvent.fromThrowable(other).handle();
            writeError(exchange, new BeaconServerErrorResponse(other), 500);
            return;
        }

            try {
                if (response != null) {
                TrackEvent.trace("Sending response:\n" + object);
                var tree = JacksonMapper.getDefault().valueToTree(response);
                TrackEvent.trace("Sending raw response:\n" + tree.toPrettyString());
                var bytes = tree.toPrettyString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                } else {
                    exchange.sendResponseHeaders(200, -1);
                }
            } catch (IOException ioException) {
                ErrorEvent.fromThrowable(ioException).omit().expected().handle();
            } catch (Throwable other) {
                ErrorEvent.fromThrowable(other).handle();
                writeError(exchange, new BeaconServerErrorResponse(other), 500);
                return;
            }
    }

    private void writeError(HttpExchange exchange, Object errorMessage, int code) {
        try {
            var bytes = JacksonMapper.getDefault().writeValueAsString(errorMessage).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException ex) {
            ErrorEvent.fromThrowable(ex).omit().expected().handle();
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private <REQ> REQ createDefaultRequest(BeaconInterface<?> beaconInterface) {
        var c = beaconInterface.getRequestClass().getDeclaredMethod("builder");
        c.setAccessible(true);
        var b = c.invoke(null);
        var m = b.getClass().getDeclaredMethod("build");
        m.setAccessible(true);
        return (REQ) beaconInterface.getRequestClass().cast(m.invoke(b));
    }
}
