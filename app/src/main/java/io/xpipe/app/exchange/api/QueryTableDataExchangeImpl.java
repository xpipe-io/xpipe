package io.xpipe.app.exchange.api;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.api.QueryTableDataExchange;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.TableReadConnection;

public class QueryTableDataExchangeImpl extends QueryTableDataExchange
        implements MessageExchangeImpl<QueryTableDataExchange.Request, QueryTableDataExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = getSourceEntry(msg.getRef(), DataSourceType.TABLE, false);

        TableReadConnection readConnection = null;
        TupleType dataType;
        try {
            readConnection = (TableReadConnection) ds.getDataSource().openReadConnection();
            readConnection.init();
            dataType = readConnection.getDataType();
            TableReadConnection finalReadConnection = readConnection;
            handler.postResponse(() -> {
                try (var out = handler.sendBody()) {
                    try (var con = finalReadConnection) {
                        con.forwardRows(out, msg.getMaxRows());
                    }
                }
            });
        } catch (Exception ex) {
            if (readConnection != null) {
                readConnection.close();
            }
            throw ex;
        }
        return QueryTableDataExchange.Response.builder().dataType(dataType).build();
    }
}
