package io.xpipe.beacon.message;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class MessageProviders {

    private static Set<MessageProvider> ALL;

    private static void loadAll() {
        if (ALL == null) {
            ALL = ServiceLoader.load(MessageProvider.class).stream()
                    .map(ServiceLoader.Provider::get).collect(Collectors.toSet());
        }
    }

    public static <RQ extends RequestMessage, RP extends ResponseMessage> Optional<MessageProvider<RQ, RP>> byId(String name) {
        loadAll();
        var r = ALL.stream().filter(d -> d.getId().equals(name)).findAny();
        return Optional.ofNullable(r.orElse(null));
    }


    public static <RQ extends RequestMessage, RP extends ResponseMessage> Optional<MessageProvider<RQ, RP>> byRequest(RQ req) {
        loadAll();
        var r = ALL.stream().filter(d -> d.getRequestClass().equals(req.getClass())).findAny();
        return Optional.ofNullable(r.orElse(null));
    }

    public static <RQ extends RequestMessage, RP extends ResponseMessage> Optional<MessageProvider<RQ, RP>> byResponse(RP rep) {
        loadAll();
        var r = ALL.stream().filter(d -> d.getResponseClass().equals(rep.getClass())).findAny();
        return Optional.ofNullable(r.orElse(null));
    }

    public static Set<MessageProvider> getAll() {
        loadAll();
        return ALL;
    }
}
