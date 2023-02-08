package io.xpipe.app.update;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.XPipeInstance;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.event.ErrorEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class XPipeInstanceHelper {

    public static UUID getInstanceId() {
        var file = AppProperties.get().getDataDir().resolve("instance");
        if (!Files.exists(file)) {
            var id = UUID.randomUUID();
            try {
                Files.writeString(file, id.toString());
            } catch (IOException e) {
                ErrorEvent.fromThrowable(e).handle();
            }
            return id;
        }

        try {
            var read = UUID.fromString(Files.readString(file));
            return read;
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return UUID.randomUUID();
        }
    }

    public static boolean isSupported(ShellStore host) {
        try (var pc = host.create().start();
                var cmd = pc.command(List.of("xpipe"))) {
            cmd.discardOrThrow();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Optional<XPipeInstance> getInstance(ShellStore store) {
        if (!isSupported(store)) {
            return Optional.empty();
        }

        //        try (BeaconClient beaconClient = ProcessBeaconClient.create(store)) {
        //            beaconClient.sendRequest(InstanceExchange.Request.builder().build());
        //            InstanceExchange.Response response = beaconClient.receiveResponse();
        //            return Optional.of(response.getInstance());
        //        } catch (Exception e) {
        //            return Optional.empty();
        //        }
        return null;
    }

    public static XPipeInstance refresh() {
        Map<ShellStore, Optional<XPipeInstance>> map = DataStorage.get().getStores().stream()
                .filter(entry -> entry.getStore() instanceof ShellStore)
                .collect(Collectors.toMap(
                        entry -> entry.getStore().asNeeded(),
                        entry -> getInstance(entry.getStore().asNeeded())));
        var adjacent = map.entrySet().stream()
                .filter(shellStoreOptionalEntry ->
                        shellStoreOptionalEntry.getValue().isPresent())
                .collect(Collectors.toMap(
                        entry -> entry.getKey(), entry -> entry.getValue().get()));
        var reachable = adjacent.values().stream()
                .map(XPipeInstance::getReachable)
                .flatMap(Collection::stream)
                .toList();

        var id = getInstanceId();
        var name = "test";
        return new XPipeInstance(id, name, adjacent, reachable);
    }
}
