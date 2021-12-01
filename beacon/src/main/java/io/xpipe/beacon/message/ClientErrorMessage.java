package io.xpipe.beacon.message;

import io.xpipe.beacon.ClientException;

public record ClientErrorMessage(String message) {

    public ClientException throwException() {
        return new ClientException(message);
    }
}
