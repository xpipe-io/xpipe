package io.xpipe.beacon;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BeaconConnection implements AutoCloseable {

    protected BeaconClient socket;

    private InputStream bodyInput;
    private OutputStream bodyOutput;

    protected abstract void constructSocket();

    @Override
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
            socket = null;
        } catch (Exception e) {
            socket = null;
            throw new BeaconException("Could not close beacon connection", e);
        }
    }

    public void withOutputStream(BeaconClient.FailableConsumer<OutputStream, IOException> ex) {
        try {
            ex.accept(getOutputStream());
        } catch (IOException e) {
            throw new BeaconException("Could not write to beacon output stream", e);
        }
    }

    public void withInputStream(BeaconClient.FailableConsumer<InputStream, IOException> ex) {
        try {
            ex.accept(getInputStream());
        } catch (IOException e) {
            throw new BeaconException("Could not read from beacon output stream", e);
        }
    }

    public void checkClosed() {
        if (socket == null) {
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
            REQ req,
            BeaconClient.FailableBiConsumer<RES, InputStream, IOException> responseConsumer) {
        checkClosed();

        performInputOutputExchange(req, null, responseConsumer);
    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> void performInputOutputExchange(
            REQ req,
            BeaconClient.FailableConsumer<OutputStream, IOException> reqWriter,
            BeaconClient.FailableBiConsumer<RES, InputStream, IOException> responseConsumer) {
        checkClosed();

        try {
            socket.sendRequest(req);
            if (reqWriter != null) {
                try (var out = socket.sendBody()) {
                    reqWriter.accept(out);
                }
            }
            RES res = socket.receiveResponse();
            try (var in = socket.receiveBody()) {
                responseConsumer.accept(res, in);
            }
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }

    public <REQ extends RequestMessage> void sendRequest(
            REQ req) {
        checkClosed();

        try {
            socket.sendRequest(req);
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }

    public <RES extends ResponseMessage> RES receiveResponse() {
        checkClosed();

        try {
            return socket.receiveResponse();
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }

    public OutputStream sendBody() {
        checkClosed();

        try {
            bodyOutput = socket.sendBody();
            return bodyOutput;
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }

    public InputStream receiveBody() {
        checkClosed();

        try {
            bodyInput = socket.receiveBody();
            return bodyInput;
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> RES performOutputExchange(
            REQ req,
            BeaconClient.FailableConsumer<OutputStream, IOException> reqWriter) {
        checkClosed();

        try {
            socket.sendRequest(req);
            try (var out = socket.sendBody()) {
                reqWriter.accept(out);
            }
            return socket.receiveResponse();
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> RES performSimpleExchange(
            REQ req) {
        checkClosed();

        try {
            socket.sendRequest(req);
            return socket.receiveResponse();
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }
}
