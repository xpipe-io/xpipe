package io.xpipe.beacon;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import org.apache.commons.lang3.function.FailableBiConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public abstract class BeaconConnector {

    protected abstract void waitForStartup();

    protected BeaconClient constructSocket() throws ConnectorException {
        if (!BeaconServer.isRunning()) {
            try {
                BeaconServer.start();
                waitForStartup();
                if (!BeaconServer.isRunning()) {
                    throw new ConnectorException("Unable to start xpipe daemon");
                }
            } catch (Exception ex) {
                throw new ConnectorException("Unable to start xpipe daemon: " + ex.getMessage());
            }
        }

        try {
            return new BeaconClient();
        } catch (Exception ex) {
            throw new ConnectorException("Unable to connect to running xpipe daemon: " + ex.getMessage());
        }
    }

    protected <REQ extends RequestMessage, RES extends ResponseMessage> void performExchange(
            BeaconClient socket,
            REQ req,
            FailableBiConsumer<RES, InputStream, IOException> responseConsumer,
            boolean keepOpen) throws ServerException, ConnectorException, ClientException {
        performExchange(socket, req, null, responseConsumer, keepOpen);
    }

    protected <REQ extends RequestMessage, RES extends ResponseMessage> void performExchange(
            BeaconClient socket,
            REQ req,
            Consumer<OutputStream> output,
            FailableBiConsumer<RES, InputStream, IOException> responseConsumer,
            boolean keepOpen) throws ServerException, ConnectorException, ClientException {
        socket.exchange(req, output, responseConsumer, keepOpen);
    }

    protected <REQ extends RequestMessage, RES extends ResponseMessage> RES performSimpleExchange(
            BeaconClient socket,
            REQ req) throws ServerException, ConnectorException, ClientException {
        return socket.simpleExchange(req);
    }
}
