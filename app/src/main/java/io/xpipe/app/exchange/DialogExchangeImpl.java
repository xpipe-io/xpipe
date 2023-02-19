package io.xpipe.app.exchange;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.DialogExchange;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.DialogReference;
import org.apache.commons.lang3.function.FailableConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DialogExchangeImpl extends DialogExchange
        implements MessageExchangeImpl<DialogExchange.Request, DialogExchange.Response> {

    private static final Map<UUID, Dialog> openDialogs = new HashMap<>();
    private static final Map<UUID, FailableConsumer<?, Exception>> openDialogConsumers = new HashMap<>();

    public static <T> DialogReference add(Dialog d, FailableConsumer<T, Exception> onCompletion) throws Exception {
        return add(d, UUID.randomUUID(), onCompletion);
    }

    public static <T> DialogReference add(Dialog d, UUID uuid, FailableConsumer<T, Exception> onCompletion)
            throws Exception {
        openDialogs.put(uuid, d);
        openDialogConsumers.put(uuid, onCompletion);
        return new DialogReference(uuid, d.start());
    }

    @Override
    public DialogExchange.Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        if (msg.isCancel()) {
            TrackEvent.withTrace("beacon", "Received cancel dialog request")
                    .tag("key", msg.getDialogKey())
                    .handle();
            openDialogs.remove(msg.getDialogKey());
            openDialogConsumers.remove(msg.getDialogKey());
            return DialogExchange.Response.builder().element(null).build();
        }

        var dialog = openDialogs.get(msg.getDialogKey());
        var e = dialog.receive(msg.getValue());

        TrackEvent.withTrace("beacon", "Received normal dialog request")
                .tag("key", msg.getDialogKey())
                .tag("value", msg.getValue())
                .tag("newElement", e)
                .handle();

        if (e == null) {
            openDialogs.remove(msg.getDialogKey());
            var con = openDialogConsumers.remove(msg.getDialogKey());
            con.accept(dialog.getResult());
        }

        return DialogExchange.Response.builder().element(e).build();
        //
        //
        //        var provider = getProvider(msg.getInstance().getProvider());
        //        var completeConfig = toCompleteConfig(provider);
        //
        //        var option = completeConfig.keySet().stream()
        //                .filter(o -> o.getKey().equals(msg.getKey())).findAny()
        //                .orElseThrow(() -> new ClientException("Invalid config key: " + msg.getKey()));
        //
        //        String errorMsg = null;
        //        try {
        //            option.getConverter().convertFromString(msg.getValue());
        //        } catch (Exception ex) {
        //            errorMsg = ex.getMessage();
        //        }
        //
        //        return DialogExchange.Response.builder().errorMsg(errorMsg).build();
    }
}
