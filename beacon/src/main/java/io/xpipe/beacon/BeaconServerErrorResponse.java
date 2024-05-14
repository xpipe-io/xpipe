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
public class BeaconServerErrorResponse {

    Throwable error;

    public void throwError() throws BeaconServerException {
        throw new BeaconServerException(error.getMessage(), error);
    }
}
