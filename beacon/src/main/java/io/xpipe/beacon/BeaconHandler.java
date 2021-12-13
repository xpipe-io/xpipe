package io.xpipe.beacon;

import io.xpipe.beacon.message.ResponseMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface BeaconHandler {

    void prepareBody() throws IOException;

    public <T extends ResponseMessage> void sendResponse(T obj) throws Exception;

    public void sendClientErrorResponse(String message) throws Exception;

    public void sendServerErrorResponse(Throwable ex) throws Exception;

    InputStream getInputStream() throws Exception;

    OutputStream getOutputStream() throws Exception;
}
