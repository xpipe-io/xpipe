package io.xpipe.beacon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@SuppressWarnings("ClassCanBeRecord")
@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class BeaconClientErrorResponse {

    String message;

    public BeaconClientException throwException() {
        return new BeaconClientException(message);
    }
}
