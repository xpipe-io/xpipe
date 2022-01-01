package io.xpipe.api;

import io.xpipe.beacon.*;

import java.util.Optional;

public abstract class XPipeApiConnector extends BeaconConnector {

    public void execute() {
        try {
            var socket = constructSocket();
            handle(socket);
        } catch (ConnectorException ce) {
            throw new XPipeConnectException(ce.getMessage());
        } catch (ClientException ce) {
            throw new XPipeClientException(ce.getMessage());
        } catch (ServerException se) {
            throw new XPipeServerException(se.getMessage());
        } catch (Throwable t) {
            throw new XPipeConnectException(t);
        }
    }

    protected abstract void handle(BeaconClient sc) throws Exception;

    @Override
    protected BeaconClient constructSocket() throws ConnectorException {
        if (!BeaconServer.isRunning()) {
            try {
                start();
            } catch (Exception ex) {
                throw new ConnectorException("Unable to start xpipe daemon", ex);
            }

            var r = waitForStartup();
            if (r.isEmpty()) {
                throw new ConnectorException("Wait for xpipe daemon timed out");
            } else {
                return r.get();
            }
        }

        try {
            return new BeaconClient();
        } catch (Exception ex) {
            throw new ConnectorException("Unable to connect to running xpipe daemon", ex);
        }
    }

    private void start() throws Exception {
        if (!BeaconServer.tryStart()) {
            throw new UnsupportedOperationException("Unable to determine xpipe daemon launch command");
        };
    }

    private Optional<BeaconClient> waitForStartup() {
        for (int i = 0; i < 40; i++) {
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

    @FunctionalInterface
    public static interface Handler {

        void handle(BeaconClient sc) throws ClientException, ServerException;
    }
}
