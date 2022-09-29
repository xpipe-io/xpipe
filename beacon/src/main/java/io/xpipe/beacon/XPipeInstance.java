package io.xpipe.beacon;

import io.xpipe.core.store.ShellStore;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
public class XPipeInstance {

    UUID uuid;
    String name;
    Map<ShellStore, XPipeInstance> adjacent;
    List<XPipeInstance> reachable;
}
