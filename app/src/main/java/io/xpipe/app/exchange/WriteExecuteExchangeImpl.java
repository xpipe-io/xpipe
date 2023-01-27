package io.xpipe.app.exchange;

import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.WriteExecuteExchange;
import io.xpipe.core.impl.OutputStreamStore;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.StreamDataStore;

public class WriteExecuteExchangeImpl extends WriteExecuteExchange
        implements MessageExchangeImpl<WriteExecuteExchange.Request, WriteExecuteExchange.Response> {

    @Override
    @SuppressWarnings("unchecked")
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = getSourceEntry(msg.getRef(), null, false);
        var target = WritePreparationExchangeImpl.CONFIGS.remove(msg.getId());

        if (target.getType() != ds.getSource().getType()) {
            throw new ClientException("Incompatible data source types");
        }

        var local = target.getStore() instanceof StreamDataStore s && s.isContentExclusivelyAccessible();

        var mode = msg.getMode() != null ? WriteMode.byId(msg.getMode()) : WriteMode.REPLACE;
        if (local) {
            handler.postResponse(() -> {
                var usedStore = new OutputStreamStore(handler.sendBody());
                var out = ((DataSource<StreamDataStore>) target).withStore(usedStore);
                try (var con = ds.getSource().openReadConnection();
                        var outCon = out.openWriteConnection(mode)) {
                    con.init();
                    outCon.init();
                    con.forward(outCon);
                }
            });
            return WriteExecuteExchange.Response.builder().hasBody(true).build();
        } else {
            var out = target;
            try (var con = ds.getSource().openReadConnection();
                    var outCon = out.openWriteConnection(mode)) {
                con.init();
                outCon.init();
                con.forward(outCon);
            }
            return WriteExecuteExchange.Response.builder().build();
        }
    }
}
