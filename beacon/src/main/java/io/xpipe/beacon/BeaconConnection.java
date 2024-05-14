package io.xpipe.beacon;

import lombok.Getter;

@Getter
public abstract class BeaconConnection implements AutoCloseable {

    protected BeaconClient beaconClient;

    protected abstract void establishConnection() throws Exception;

    @Override
    public void close() {
        beaconClient = null;
    }

    public void checkClosed() {
        if (beaconClient == null) {
            throw new IllegalStateException("Socket is closed");
        }
    }

    public <REQ, RES> RES performSimpleExchange(REQ req) throws Exception {
        checkClosed();
        return beaconClient.performRequest(req);
    }
}
