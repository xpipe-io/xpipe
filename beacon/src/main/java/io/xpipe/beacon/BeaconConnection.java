package io.xpipe.beacon;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BeaconConnection implements AutoCloseable {

    protected BeaconClient socket;

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

    public void closeOutput() {
        try {
            socket.getOutputStream().close();
        } catch (Exception e) {
            throw new BeaconException("Could not close beacon output stream", e);
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

        return socket.getOutputStream();
    }

    public InputStream getInputStream() {
        checkClosed();

        return socket.getInputStream();
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
            socket.exchange(req, reqWriter, responseConsumer);
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

    public void sendBodyStart() {
        checkClosed();

        try {
            socket.startBody();
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }

    public void receiveBody() {
        checkClosed();

        try {
            socket.receiveBody();
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
            socket.startBody();
            reqWriter.accept(socket.getOutputStream());
            return socket.receiveResponse();
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }

//    public void writeLength(int bytes) throws IOException {
//        checkClosed();
//        socket.getOutputStream().write(ByteBuffer.allocate(4).putInt(bytes).array());
//    }

    public <REQ extends RequestMessage, RES extends ResponseMessage> RES performSimpleExchange(
            REQ req) {
        checkClosed();

        try {
            return socket.simpleExchange(req);
        } catch (Exception e) {
            throw new BeaconException("Could not communicate with beacon", e);
        }
    }
}
