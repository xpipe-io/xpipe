package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class MessageExchanges {

    private static Set<MessageExchange> ALL;

    private static void loadAll() {
        if (ALL == null) {
            ALL = ServiceLoader.load(MessageExchange.class).stream()
                    .map(ServiceLoader.Provider::get).collect(Collectors.toSet());
        }
    }

    public static Optional<MessageExchange> byId(String name) {
        loadAll();
        return ALL.stream().filter(d -> d.getId().equals(name)).findAny();
    }

    public static <RQ extends RequestMessage> Optional<MessageExchange> byRequest(RQ req) {
        loadAll();
        return ALL.stream().filter(d -> d.getRequestClass().equals(req.getClass())).findAny();
    }

    public static <RP extends ResponseMessage> Optional<MessageExchange> byResponse(RP rep) {
        loadAll();
        return ALL.stream().filter(d -> d.getResponseClass().equals(rep.getClass())).findAny();
    }

    public static Set<MessageExchange> getAll() {
        loadAll();
        return ALL;
    }
}
