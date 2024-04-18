package io.xpipe.beacon.exchange.data;

import io.xpipe.beacon.ServerException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@SuppressWarnings("ClassCanBeRecord")
@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class ServerErrorMessage {

    UUID requestId;
    Throwable error;

    public void throwError() throws ServerException {
        throw new ServerException(error.getMessage(), error);
    }
}
