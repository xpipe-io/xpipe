package io.xpipe.app.vnc;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ClipboardHelper;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.SecretValue;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ExternalVncClient {

    static void launchClient(LaunchConfiguration configuration) throws Exception {
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
        l.add(TightVncClient.class);
        switch (OsType.getLocal()) {
            case OsType.Linux linux -> {
                l.add(RealVncClient.Linux.class);
                l.add(TigerVncClient.Linux.class);
            }
            case OsType.MacOs macOs -> {
                l.add(RealVncClient.MacOs.class);
                l.add(TigerVncClient.MacOs.class);
            }
            case OsType.Windows windows -> {
                l.add(RealVncClient.Windows.class);
                l.add(TigerVncClient.Windows.class);
            }
        }
        l.add(CustomVncClient.class);
        return l;
    }

    @Value
    class LaunchConfiguration {
        String title;
        String host;
        int port;
        DataStoreEntryRef<VncBaseStore> entry;
        ShellControl shellControl;

        public Optional<String> retrieveUsername() {
            return Optional.ofNullable(entry.getStore().getUser());
        }

        public boolean hasFixedPassword() {
            return entry.getStore().getPassword() != null && entry.getStore().getPassword().expectsQuery() &&
                    !entry.getStore().getPassword().query().requiresUserInteraction();
        }

        public boolean isTunneled() {
            return shellControl != null;
        }

        public Optional<SecretValue> retrievePassword() {
            var strat = entry.getStore().getPassword();
            if (!strat.expectsQuery()) {
                return Optional.empty();
            }

            var r = strat.query().query("Password for " + title);
            return Optional.ofNullable(r.getSecret());
        }
    }

    void launch(LaunchConfiguration configuration) throws Exception;

    boolean supportsPasswords();
}
