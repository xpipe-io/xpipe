package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.ConnectionQueryExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConnectionQueryExchangeImpl extends ConnectionQueryExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws IOException, BeaconClientException, BeaconServerException {
        var catMatcher = Pattern.compile(toRegex(msg.getCategoryFilter()));
        var conMatcher = Pattern.compile(toRegex(msg.getConnectionFilter()));

        List<DataStoreEntry> found = new ArrayList<>();
        for (DataStoreEntry storeEntry : DataStorage.get().getStoreEntries()) {
            if (!storeEntry.getValidity().isUsable()) {
                continue;
            }

            var name = DataStorage.get().getStorePath(storeEntry).toString();
            if (!conMatcher.matcher(name).matches()) {
                continue;
            }

            var cat = DataStorage.get().getStoreCategoryIfPresent(storeEntry.getCategoryUuid()).orElse(null);
            if (cat == null) {
                continue;
            }

            var c = DataStorage.get().getStorePath(cat).toString();
            if (!catMatcher.matcher(c).matches()) {
                continue;
            }

            found.add(storeEntry);
        }

        var mapped = new ArrayList<QueryResponse>();
        for (DataStoreEntry e : found) {
            var cat = DataStorage.get().getStorePath(DataStorage.get().getStoreCategoryIfPresent(e.getCategoryUuid()).orElseThrow());
            var obj = ConnectionQueryExchange.QueryResponse.builder()
                    .uuid(e.getUuid()).category(cat).connection(DataStorage.get()
                    .getStorePath(e)).type(e.getProvider().getId()).build();
            mapped.add(obj);
        }
        return Response.builder().found(mapped).build();
    }

    private String toRegex(String pattern) {
        return pattern.replaceAll("\\*\\*", ".*?").replaceAll("\\*","[^\\\\]*?");
    }
}
