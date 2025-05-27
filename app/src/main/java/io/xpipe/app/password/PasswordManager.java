package io.xpipe.app.password;

import io.xpipe.core.process.OsType;
import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.ValidationException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface PasswordManager {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(OnePasswordManager.class);
        l.add(KeePassXcManager.class);
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

    static class CredentialResult {

        String username;
        SecretValue password;
    }

    default void checkComplete() throws ValidationException {}

    String retrievePassword(String key);

    String getKeyPlaceholder();
}
