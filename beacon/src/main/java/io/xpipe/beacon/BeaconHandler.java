package io.xpipe.beacon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An exchange handler responsible for properly handling a request and sending a response.
 */
public interface BeaconHandler {

    /**
     * Execute a Runnable after the initial response has been sent.
     *
     * @param r the runnable to execute
     */
    void postResponse(BeaconClient.FailableRunnable<Exception> r);

    /**
     * Prepares to attach a body to a response.
     *
     * @return the output stream that can be used to write the body payload
     */
    OutputStream sendBody() throws IOException;

    /**
     * Prepares to read an attached body of a request.
     *
     * @return the input stream that can be used to read the body payload
     */
    InputStream receiveBody() throws IOException;
}
