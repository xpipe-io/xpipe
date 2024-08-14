package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.TerminalLauncherManager;
import io.xpipe.beacon.api.SshLaunchExchange;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.TerminalInitScriptConfig;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.store.StorePath;

import java.util.UUID;

public class SshLaunchExchangeImpl extends SshLaunchExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Exception {
        if (msg.getStorePath() != null && !msg.getStorePath().contains("SSH_ORIGINAL_COMMAND")) {
            var storePath = StorePath.create(msg.getStorePath());
            var found = DataStorage.get().getStoreEntries().stream().filter(entry -> DataStorage.get().getStorePath(entry).equals(storePath)).findFirst();
            if (found.isPresent() && found.get().getStore() instanceof ShellStore shellStore) {
                TerminalLauncherManager.submitAsync(UUID.randomUUID(), shellStore.control(),
                        TerminalInitScriptConfig.ofName(DataStorage.get().getStoreEntryDisplayName(found.get())),null);
            }
        }

        var r = TerminalLauncherManager.waitForNextLaunch();
        var c = ProcessControlProvider.get().getEffectiveLocalDialect().getOpenScriptCommand(r.toString()).buildBaseParts(null);
        return Response.builder().command(c).build();
    }
}
