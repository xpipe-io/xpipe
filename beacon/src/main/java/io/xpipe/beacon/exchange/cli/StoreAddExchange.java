package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.core.dialog.DialogReference;
import io.xpipe.core.store.DataStore;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class StoreAddExchange implements MessageExchange {

    @Override
    public String getId() {
        return "storeAdd";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        DataStore storeInput;

        String type;
        String name;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        DialogReference config;
    }
}
