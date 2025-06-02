package io.xpipe.app.vnc;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.app.pwman.*;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.SecretValue;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ExternalVncClient {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(OnePasswordManager.class);
        l.add(KeePassXcPasswordManager.class);
        l.add(BitwardenPasswordManager.class);
        l.add(DashlanePasswordManager.class);
        if (OsType.getLocal() != OsType.WINDOWS) {
            l.add(LastpassPasswordManager.class);
            l.add(EnpassPasswordManager.class);
        }
        l.add(KeeperPasswordManager.class);
        l.add(PsonoPasswordManager.class);
        if (OsType.getLocal() == OsType.WINDOWS) {
            l.add(WindowsCredentialManager.class);
        }
        l.add(PasswordManagerCommand.class);
        return l;
    }

    @Value
    class LaunchConfiguration {
        String title;
        String host;
        int port;
        SecretValue password;
    }

    boolean isAvailable();

    void launch(LaunchConfiguration configuration) throws Exception;

}
