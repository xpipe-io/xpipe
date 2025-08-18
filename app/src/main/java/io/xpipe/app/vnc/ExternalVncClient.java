package io.xpipe.app.vnc;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ClipboardHelper;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ExternalVncClient {

    static void launchClient(VncLaunchConfig configuration) throws Exception {
        var client = AppPrefs.get().vncClient.getValue();
        if (client == null) {
            return;
        }

        if (!client.supportsPasswords() && configuration.hasFixedPassword()) {
            var pw = configuration.retrievePassword();
            if (pw.isPresent()) {
                ClipboardHelper.copyPassword(pw.get());
            }
        }

        client.launch(configuration);
    }

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(InternalVncClient.class);
        switch (OsType.getLocal()) {
            case OsType.Linux linux -> {
                l.add(RemminaVncClient.class);
                l.add(TigerVncClient.Linux.class);
                l.add(RealVncClient.Linux.class);
            }
            case OsType.MacOs macOs -> {
                l.add(ScreenSharingVncClient.class);
                l.add(TigerVncClient.MacOs.class);
                l.add(RealVncClient.MacOs.class);
            }
            case OsType.Windows windows -> {
                l.add(TigerVncClient.Windows.class);
                l.add(RealVncClient.Windows.class);
                l.add(TightVncClient.class);
            }
        }
        l.add(CustomVncClient.class);
        return l;
    }

    void launch(VncLaunchConfig configuration) throws Exception;

    boolean supportsPasswords();
}
