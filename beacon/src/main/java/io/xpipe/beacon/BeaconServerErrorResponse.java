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
    String documentationLink;

    public void throwError() throws BeaconServerException {
        var message = error.getMessage();
        if (documentationLink != null) {
            message = message + "\n\nFor more information, see: " + documentationLink;
        }
        throw new BeaconServerException(message, error);
    }
}
