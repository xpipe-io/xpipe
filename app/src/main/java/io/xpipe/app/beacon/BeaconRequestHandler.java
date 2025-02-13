package io.xpipe.app.beacon;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.*;
import io.xpipe.core.util.JacksonMapper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BeaconRequestHandler<T> implements HttpHandler {

    private final BeaconInterface<T> beaconInterface;

    public BeaconRequestHandler(BeaconInterface<T> beaconInterface) {
        this.beaconInterface = beaconInterface;
    }

    @Override
    public void handle(HttpExchange exchange) {
        if (OperationMode.isInShutdown() && !beaconInterface.acceptInShutdown()) {
            writeError(exchange, new BeaconClientErrorResponse("Daemon is currently in shutdown"), 400);
            return;
        }

        if (beaconInterface.requiresCompletedStartup()) {
            while (OperationMode.isInStartup()) {
                ThreadHelper.sleep(100);
            }
        }

        if (beaconInterface.requiresEnabledApi()
                && !AppPrefs.get().enableHttpApi().get()) {
            var ex = new BeaconServerException("HTTP API is not enabled in the settings menu");
            writeError(exchange, ex, 403);
            return;
        }

        if (!AppPrefs.get().disableApiAuthentication().get() && beaconInterface.requiresAuthentication()) {
            var auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null) {
                writeError(exchange, new BeaconClientErrorResponse("Missing Authorization header"), 401);
                return;
            }

            var token = auth.replace("Bearer ", "");
            var session = AppBeaconServer.get().getSessions().stream()
                    .filter(s -> s.getToken().equals(token))
                    .findFirst()
                    .orElse(null);
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
            if (beaconInterface.readRawRequestBody()) {
                object = createDefaultRequest(beaconInterface);
            } else {
                try (InputStream is = exchange.getRequestBody()) {
                    var read = is.readAllBytes();
                    var rawDataRequestClass = beaconInterface.getRequestClass().getDeclaredFields().length == 1
                            && beaconInterface
                                    .getRequestClass()
                                    .getDeclaredFields()[0]
                                    .getType()
                                    .equals(byte[].class);
                    if (!new String(read, StandardCharsets.US_ASCII).trim().startsWith("{") && rawDataRequestClass) {
                        object = createRawDataRequest(beaconInterface, read);
                    } else {
                        var tree = JacksonMapper.getDefault().readTree(read);
                        TrackEvent.trace("Parsed raw request:\n" + tree.toPrettyString());
                        var emptyRequestClass = tree.isEmpty()
                                && beaconInterface.getRequestClass().getDeclaredFields().length == 0;
                        object = emptyRequestClass
                                ? createDefaultRequest(beaconInterface)
                                : JacksonMapper.getDefault().treeToValue(tree, beaconInterface.getRequestClass());
                        TrackEvent.trace("Parsed request object:\n" + object);
                    }
                }
            }

            var sync = beaconInterface.getSynchronizationObject();
            if (sync != null) {
                synchronized (sync) {
                    response = beaconInterface.handle(exchange, object);
                }
            } else {
                response = beaconInterface.handle(exchange, object);
            }
        } catch (BeaconClientException clientException) {
            ErrorEvent.fromThrowable(clientException).omit().expected().handle();
            writeError(exchange, new BeaconClientErrorResponse(clientException.getMessage()), 400);
            return;
        } catch (BeaconServerException serverException) {
            var cause = serverException.getCause() != null ? serverException.getCause() : serverException;
            ErrorEvent.fromThrowable(cause).omit().handle();
            writeError(exchange, new BeaconServerErrorResponse(cause), 500);
            return;
        } catch (IOException ex) {
            // Handle serialization errors as normal exceptions and other IO exceptions as assuming that the connection
            // is broken
            if (!ex.getClass().getName().contains("jackson")) {
                ErrorEvent.fromThrowable(ex).omit().expected().handle();
            } else {
                ErrorEvent.fromThrowable(ex).omit().expected().handle();
                // Make deserialization error message more readable
                var message = ex.getMessage()
                        .replace("$RequestBuilder", "")
                        .replace("Exchange$Request", "Request")
                        .replace("at [Source: UNKNOWN; byte offset: #UNKNOWN]", "")
                        .replaceAll("(\\w+) is marked non-null but is null", "field $1 is missing from object")
                        .trim();
                writeError(exchange, new BeaconClientErrorResponse(message), 400);
            }
            return;
        } catch (Throwable other) {
            ErrorEvent.fromThrowable(other).omit().expected().handle();
            writeError(exchange, new BeaconServerErrorResponse(other), 500);
            return;
        }

        try {
            var emptyResponseClass = beaconInterface.getResponseClass().getDeclaredFields().length == 0;
            if (!emptyResponseClass && response != null) {
                TrackEvent.trace("Sending response:\n" + response);
                TrackEvent.trace("Sending raw response:\n"
                        + JacksonMapper.getCensored().valueToTree(response).toPrettyString());
                var bytes = JacksonMapper.getDefault()
                        .valueToTree(response)
                        .toPrettyString()
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.sendResponseHeaders(200, -1);
            }
        } catch (IOException ioException) {
            // The exchange implementation might have already sent a response manually
            if (!"headers already sent".equals(ioException.getMessage())) {
                ErrorEvent.fromThrowable(ioException).omit().expected().handle();
            }
        } catch (Throwable other) {
            ErrorEvent.fromThrowable(other).handle();
            writeError(exchange, new BeaconServerErrorResponse(other), 500);
        }
    }

    private void writeError(HttpExchange exchange, Object errorMessage, int code) {
        try {
            var bytes =
                    JacksonMapper.getDefault().writeValueAsString(errorMessage).getBytes(StandardCharsets.UTF_8);
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

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private <REQ> REQ createRawDataRequest(BeaconInterface<?> beaconInterface, byte[] s) {
        var c = beaconInterface.getRequestClass().getDeclaredMethod("builder");
        c.setAccessible(true);

        var b = c.invoke(null);
        var setMethod = Arrays.stream(b.getClass().getDeclaredMethods())
                .filter(method -> method.getParameterCount() == 1
                        && method.getParameters()[0].getType().equals(byte[].class))
                .findFirst()
                .orElseThrow();
        setMethod.invoke(b, (Object) s);

        var m = b.getClass().getDeclaredMethod("build");
        m.setAccessible(true);
        return (REQ) beaconInterface.getRequestClass().cast(m.invoke(b));
    }
}
