package io.xpipe.beacon.message;

import io.xpipe.beacon.ServerException;

import java.util.UUID;

public record ServerErrorMessage(UUID requestId, Throwable error) {

    public void throwError() throws ServerException {
        throw new ServerException(error.getMessage(), error);
    }
}
