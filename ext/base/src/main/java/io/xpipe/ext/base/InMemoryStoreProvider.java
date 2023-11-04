package io.xpipe.ext.base;

import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.InMemoryStore;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class InMemoryStoreProvider implements DataStoreProvider {

    @Override
    public Dialog dialogForStore(DataStore store) {
        InMemoryStore s = store.asNeeded();
        var userQ = Dialog.query("Value", true, true, false, new String(s.getValue(), StandardCharsets.UTF_8), QueryConverter.STRING);
        return userQ.evaluateTo(() -> {
            byte[] bytes = ((String) userQ.getResult()).getBytes(StandardCharsets.UTF_8);
            return new InMemoryStore(bytes);
        });
    }

    @Override
    public DataStore defaultStore() {
        return new InMemoryStore(new byte[0]);
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("inMemory");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(InMemoryStore.class);
    }
}
