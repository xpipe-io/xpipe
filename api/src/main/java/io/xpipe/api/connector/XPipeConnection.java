package io.xpipe.api.connector;

import io.xpipe.beacon.*;
import io.xpipe.beacon.exchange.cli.DialogExchange;
import io.xpipe.core.dialog.DialogReference;
import io.xpipe.core.util.JacksonHelper;

import java.util.Optional;

public final class XPipeConnection extends BeaconConnection {

    public static XPipeConnection open() {
        var con = new XPipeConnection();
        con.constructSocket();
        return con;
    }

    public static void finishDialog(DialogReference reference) {
        try (var con = new XPipeConnection()) {
            con.constructSocket();
            while (true) {
                DialogExchange.Response response = con.performSimpleExchange(DialogExchange.Request.builder().dialogKey(reference.getDialogId()).build());
                if (response.getElement() == null) {
                    break;
                }
            }
        } catch (BeaconException e) {
            throw e;
        } catch (Exception e) {
            throw new BeaconException(e);
        }
    }

    public static void execute(Handler handler) {
        try (var con = new XPipeConnection()) {
            con.constructSocket();
            handler.handle(con);
        } catch (BeaconException e) {
            throw e;
        } catch (Exception e) {
            throw new BeaconException(e);
        }
    }

    public static <T> T execute(Mapper<T> mapper) {
        try (var con = new XPipeConnection()) {
            con.constructSocket();
            return mapper.handle(con);
        } catch (BeaconException e) {
            throw e;
        } catch (Exception e) {
            throw new BeaconException(e);
        }
    }

    private XPipeConnection() {
    }

    @Override
    protected void constructSocket() {
        if (!JacksonHelper.isInit()) {
            JacksonHelper.initModularized(ModuleLayer.boot());
        }

        if (!BeaconServer.isRunning()) {
            try {
                start();
            } catch (Exception ex) {
                throw new BeaconException("Unable to start xpipe daemon", ex);
            }

            var r = waitForStartup();
            if (r.isEmpty()) {
                throw new BeaconException("Wait for xpipe daemon timed out");
            } else {
                socket = r.get();
                return;
            }
        }

        try {
            socket = new BeaconClient();
        } catch (Exception ex) {
            throw new BeaconException("Unable to connect to running xpipe daemon", ex);
        }
    }

    private void start() throws Exception {
        if (!BeaconServer.tryStart()) {
            throw new UnsupportedOperationException("Unable to determine xpipe daemon launch command");
        };
    }

    public static Optional<BeaconClient> waitForStartup() {
        for (int i = 0; i < 80; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            var s = BeaconClient.tryConnect();
            if (s.isPresent()) {
                return s;
            }
        }
        return Optional.empty();
    }

    public static void waitForShutdown() {
        for (int i = 0; i < 40; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            var r = BeaconServer.isRunning();
            if (!r) {
                return;
            }
        }
    }

    @FunctionalInterface
    public static interface Handler {

        void handle(BeaconConnection con) throws ClientException, ServerException, ConnectorException;
    }

    @FunctionalInterface
    public static interface Mapper<T> {

        T handle(BeaconConnection con) throws ClientException, ServerException, ConnectorException;
    }
}
