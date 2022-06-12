package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class MessageExchanges {

    private static Set<MessageExchange> ALL;

    private static void loadAll() {
        if (ALL == null) {
            ALL = ServiceLoader.load(MessageExchange.class).stream()
                    .map(s -> (MessageExchange) s.get()).collect(Collectors.toSet());
        }
    }

    public static <RQ extends RequestMessage, RP extends ResponseMessage> Optional<MessageExchange> byId(String name) {
        loadAll();
        var r = ALL.stream().filter(d -> d.getId().equals(name)).findAny();
        return Optional.ofNullable((MessageExchange) r.orElse(null));
    }

    public static <RQ extends RequestMessage, RP extends ResponseMessage> Optional<MessageExchange> byRequest(RQ req) {
        loadAll();
        var r = ALL.stream().filter(d -> d.getRequestClass().equals(req.getClass())).findAny();
        return r;
    }

    public static <RQ extends RequestMessage, RP extends ResponseMessage> Optional<MessageExchange> byResponse(RP rep) {
        loadAll();
        var r = ALL.stream().filter(d -> d.getResponseClass().equals(rep.getClass())).findAny();
        return r;
    }

    public static Set<MessageExchange> getAll() {
        loadAll();
        return ALL;
    }
}
