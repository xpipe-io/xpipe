package io.xpipe.beacon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface BeaconHandler {

    void postResponse(BeaconClient.FailableRunnable<Exception> r);

    OutputStream sendBody() throws IOException;

    InputStream receiveBody() throws IOException;
}
