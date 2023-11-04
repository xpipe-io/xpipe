package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Stores a stream of data in a storage.
 */
public class ReadStreamExchange implements MessageExchange {

    @Override
    public String getId() {
        return "readStream";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull String name;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {}
}
