package io.xpipe.app.pwman;

import io.xpipe.core.OsType;
import io.xpipe.core.SecretValue;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface PasswordManager {

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
    class CredentialResult {

        String username;
        SecretValue password;
    }

    CredentialResult retrieveCredentials(String key);

    String getKeyPlaceholder();
}
