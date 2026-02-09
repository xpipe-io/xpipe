package io.xpipe.app.spice;

import io.xpipe.app.ext.PrefsValue;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ExternalSpiceClient extends PrefsValue {

    static ExternalSpiceClient determineDefault(ExternalSpiceClient existing) {
        // Verify that our selection is still valid
        if (existing != null && existing.isAvailable()) {
            return existing;
        }

        return switch (OsType.ofLocal()) {
            case OsType.Linux ignored -> {
                yield new RemoteViewerSpiceClient.Linux();
            }
            case OsType.MacOs ignored -> {
                yield new RemoteViewerSpiceClient.MacOs();
            }
            case OsType.Windows ignored -> {
                yield new RemoteViewerSpiceClient.Windows();
            }
        };
    }

    static void launchClient(SpiceLaunchConfig configuration) throws Exception {
        var client = AppPrefs.get().spiceClient.getValue();
        if (client == null) {
            return;
        }

        client.launch(configuration);
    }

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        switch (OsType.ofLocal()) {
            case OsType.Linux ignored -> {
                l.add(RemoteViewerSpiceClient.Linux.class);
            }
            case OsType.MacOs ignored -> {
                l.add(RemoteViewerSpiceClient.MacOs.class);
            }
            case OsType.Windows ignored -> {
                l.add(RemoteViewerSpiceClient.Windows.class);
            }
        }
        l.add(CustomSpiceClient.class);
        return l;
    }

    void launch(SpiceLaunchConfig configuration) throws Exception;

    String getWebsite();
}
