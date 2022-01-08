package io.xpipe.beacon;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BeaconConnector {

    protected abstract BeaconClient constructSocket() throws ConnectorException;

    protected <REQ extends RequestMessage, RES extends ResponseMessage> void performInputExchange(
            BeaconClient socket,
            REQ req,
            BeaconClient.FailableBiPredicate<RES, InputStream, IOException> responseConsumer) throws ServerException, ConnectorException, ClientException {
        performInputOutputExchange(socket, req, null, responseConsumer);
    }

    protected <REQ extends RequestMessage, RES extends ResponseMessage> void performInputOutputExchange(
            BeaconClient socket,
            REQ req,
            BeaconClient.FailableConsumer<OutputStream, IOException> reqWriter,
            BeaconClient.FailableBiPredicate<RES, InputStream, IOException> responseConsumer)
            throws ServerException, ConnectorException, ClientException {
        socket.exchange(req, reqWriter, responseConsumer);
    }

    protected <REQ extends RequestMessage, RES extends ResponseMessage> RES performOutputExchange(
            BeaconClient socket,
            REQ req,
            BeaconClient.FailableConsumer<OutputStream, IOException> reqWriter)
            throws ServerException, ConnectorException, ClientException {
        AtomicReference<RES> response = new AtomicReference<>();
        socket.exchange(req, reqWriter, (RES res, InputStream in) -> {
            response.set(res);
            return true;
        });
        return response.get();
    }

    protected void writeLength(BeaconClient socket, int bytes) throws IOException {
        socket.getOutputStream().write(ByteBuffer.allocate(4).putInt(bytes).array());
    }

    protected <REQ extends RequestMessage, RES extends ResponseMessage> RES performSimpleExchange(
            BeaconClient socket,
            REQ req) throws ServerException, ConnectorException, ClientException {
        return socket.simpleExchange(req);
    }
}
