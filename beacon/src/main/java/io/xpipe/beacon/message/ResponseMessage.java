package io.xpipe.beacon.message;

import io.xpipe.beacon.BeaconHandler;

public interface ResponseMessage {

    default void postSend(BeaconHandler handler) throws Exception {

    }
}
