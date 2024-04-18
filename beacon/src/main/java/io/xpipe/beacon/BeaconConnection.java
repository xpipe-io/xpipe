package io.xpipe.beacon;

import io.xpipe.core.util.FailableBiConsumer;
import io.xpipe.core.util.FailableConsumer;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BeaconConnection implements AutoCloseable {

    @Getter
    protected BeaconClient beaconClient;

    private InputStream bodyInput;
    private OutputStream bodyOutput;

    protected abstract void constructSocket();

    @Override
    public void close() {
        try {
            if (beaconClient != null) {
                beaconClient.close();
            }
            beaconClient = null;
        } catch (Exception e) {
            beaconClient = null;
            throw new BeaconException("Could not close beacon connection", e);
        }
    }

    public void withOutputStream(FailableConsumer<OutputStream, IOException> ex) {
        try {
            ex.accept(getOutputStream());
        } catch (IOException e) {
            throw new BeaconException("Could not write to beacon output stream", e);
        }
    }

    public void withInputStream(FailableConsumer<InputStream, IOException> ex) {
        try {
            ex.accept(getInputStream());
        } catch (IOException e) {
            throw new BeaconException("Could not read from beacon output stream", e);
        }
    }

    public void checkClosed() {
        if (beaconClient == null) {
            throw new BeaconException("Socket is closed");
        }
    }

    public OutputStream getOutputStream() {
        checkClosed();

        if (bodyOutput == null) {
            throw new IllegalStateException("Body output has not started yet");
        }

        return bodyOutput;
    }

    public InputStream getInputStream() {
        checkClosed();

        if (bodyInput == null) {
            throw new IllegalStateException("Body input has not started yet");
        }

        return bodyInput;
    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> void performInputExchange(
            REQ req, FailableBiConsumer<RES, InputStream, Exception> responseConsumer) {
        checkClosed();

        performInputOutputExchange(req, null, responseConsumer);
    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> void performInputOutputExchange(
            REQ req,
            FailableConsumer<OutputStream, IOException> reqWriter,
            FailableBiConsumer<RES, InputStream, Exception> responseConsumer) {
        checkClosed();

        try {
            beaconClient.sendRequest(req);
            if (reqWriter != null) {
                try (var out = sendBody()) {
                    reqWriter.accept(out);
                }
            }
            RES res = beaconClient.receiveResponse();
            try (var in = receiveBody()) {
                responseConsumer.accept(res, in);
            }
        } catch (Exception e) {
            throw unwrapException(e);
        }
    }

    public <REQ extends RequestMessage> void sendRequest(REQ req) {
        checkClosed();

        try {
            beaconClient.sendRequest(req);
        } catch (Exception e) {
            throw unwrapException(e);
        }
    }

    public <RES extends ResponseMessage> RES receiveResponse() {
        checkClosed();

        try {
            return beaconClient.receiveResponse();
        } catch (Exception e) {
            throw unwrapException(e);
        }
    }

    public OutputStream sendBody() {
        checkClosed();

        try {
            bodyOutput = beaconClient.sendBody();
            return bodyOutput;
        } catch (Exception e) {
            throw unwrapException(e);
        }
    }

    public InputStream receiveBody() {
        checkClosed();

        try {
            bodyInput = beaconClient.receiveBody();
            return bodyInput;
        } catch (Exception e) {
            throw unwrapException(e);
        }
    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> RES performOutputExchange(
            REQ req, FailableConsumer<OutputStream, Exception> reqWriter) {
        checkClosed();

        try {
            beaconClient.sendRequest(req);
            try (var out = sendBody()) {
                reqWriter.accept(out);
            }
            return beaconClient.receiveResponse();
        } catch (Exception e) {
            throw unwrapException(e);
        }
    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> RES performSimpleExchange(REQ req) {
        checkClosed();

        try {
            beaconClient.sendRequest(req);
            return beaconClient.receiveResponse();
        } catch (Exception e) {
            throw unwrapException(e);
        }
    }

    private BeaconException unwrapException(Exception exception) {
        if (exception instanceof ServerException s) {
            return new BeaconException("An internal server error occurred", s);
        }

        if (exception instanceof ClientException s) {
            return new BeaconException("A client error occurred", s);
        }

        if (exception instanceof ConnectorException s) {
            return new BeaconException("A beacon connection error occurred", s);
        }

        return new BeaconException("An unexpected error occurred", exception);
    }
}
