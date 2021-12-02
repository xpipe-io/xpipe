package io.xpipe.beacon.message;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class MessageExchanges {

    private static Set<MessageExchange<?,?>> ALL;

    private static void loadAll() {
        if (ALL == null) {
            ALL = ServiceLoader.load(MessageExchange.class).stream()
                    .map(s -> (MessageExchange<?,?>) s.get()).collect(Collectors.toSet());
        }
    }

    @SuppressWarnings("unchecked")
    public static <RQ extends RequestMessage, RP extends ResponseMessage> Optional<MessageExchange<RQ, RP>> byId(String name) {
        loadAll();
        var r = ALL.stream().filter(d -> d.getId().equals(name)).findAny();
        return Optional.ofNullable((MessageExchange<RQ, RP>) r.orElse(null));
    }

    @SuppressWarnings("unchecked")
    public static <RQ extends RequestMessage, RP extends ResponseMessage> Optional<MessageExchange<RQ, RP>> byRequest(RQ req) {
        loadAll();
        var r = ALL.stream().filter(d -> d.getRequestClass().equals(req.getClass())).findAny();
        return Optional.ofNullable((MessageExchange<RQ, RP>) r.orElse(null));
    }

    @SuppressWarnings("unchecked")
    public static <RQ extends RequestMessage, RP extends ResponseMessage> Optional<MessageExchange<RQ, RP>> byResponse(RP rep) {
        loadAll();
        var r = ALL.stream().filter(d -> d.getResponseClass().equals(rep.getClass())).findAny();
        return Optional.ofNullable((MessageExchange<RQ, RP>) r.orElse(null));
    }

    public static Set<MessageExchange<?,?>> getAll() {
        loadAll();
        return ALL;
    }
}
