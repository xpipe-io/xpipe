package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.api.CategoryAddExchange;
import io.xpipe.beacon.api.ConnectionAddExchange;
import io.xpipe.core.util.ValidationException;

public class CategoryAddExchangeImpl extends CategoryAddExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Throwable {
        var cat = DataStoreCategory.createNew(msg.getParent(), msg.getName());
        DataStorage.get().addStoreCategory(cat);
        return Response.builder().category(cat.getUuid()).build();
    }
}
