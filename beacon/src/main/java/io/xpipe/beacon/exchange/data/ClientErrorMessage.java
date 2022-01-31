package io.xpipe.beacon.exchange.data;

import io.xpipe.beacon.ClientException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class ClientErrorMessage {

    String message;

    public ClientException throwException() {
        return new ClientException(message);
    }
}
