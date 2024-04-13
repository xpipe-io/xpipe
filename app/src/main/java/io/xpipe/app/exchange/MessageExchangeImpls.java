package io.xpipe.app.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchanges;
import io.xpipe.core.util.ModuleLayerLoader;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class MessageExchangeImpls {

    private static List<MessageExchangeImpl<?, ?>> ALL;

    @SuppressWarnings("unchecked")
    public static <RQ extends RequestMessage, RS extends ResponseMessage> Optional<MessageExchangeImpl<RQ, RS>> byId(
            String name) {
        var r = ALL.stream().filter(d -> d.getId().equals(name)).findAny();
        return Optional.ofNullable((MessageExchangeImpl<RQ, RS>) r.orElse(null));
    }

    @SuppressWarnings("unchecked")
    public static <RQ extends RequestMessage, RS extends ResponseMessage>
            Optional<MessageExchangeImpl<RQ, RS>> byRequest(RQ req) {
        var r = ALL.stream()
                .filter(d -> d.getRequestClass().equals(req.getClass()))
                .findAny();
        return Optional.ofNullable((MessageExchangeImpl<RQ, RS>) r.orElse(null));
    }

    public static List<MessageExchangeImpl<?, ?>> getAll() {
        return ALL;
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL = ServiceLoader.load(layer, MessageExchangeImpl.class).stream()
                    .map(s -> {
                        // TrackEvent.trace("init", "Loaded exchange implementation " + ex.getId());
                        return (MessageExchangeImpl<?, ?>) s.get();
                    })
                    .collect(Collectors.toList());

            ALL.forEach(messageExchange -> {
                if (MessageExchanges.byId(messageExchange.getId()).isEmpty()) {
                    throw new AssertionError("Missing base exchange: " + messageExchange.getId());
                }
            });

            MessageExchanges.getAll().forEach(messageExchange -> {
                if (MessageExchangeImpls.byId(messageExchange.getId()).isEmpty()) {
                    throw new AssertionError("Missing exchange implementation: " + messageExchange.getId());
                }
            });
        }
    }
}
