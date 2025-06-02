package io.xpipe.app.vnc;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.app.pwman.*;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.SecretValue;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ExternalVncClient {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(InternalVncClient.class);
        l.add(TightVncClient.class);
        return l;
    }

    @Value
    class LaunchConfiguration {
        String title;
        String host;
        int port;
        DataStoreEntryRef<VncBaseStore> entry;
    }

    boolean isAvailable();

    void launch(LaunchConfiguration configuration) throws Exception;
}
