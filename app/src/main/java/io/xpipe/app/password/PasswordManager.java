package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.ValidationException;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface PasswordManager {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(NoPasswordManager.class);
        l.add(OnePasswordManager.class);
        l.add(KeePassXcManager.class);
        if (OsType.getLocal() == OsType.WINDOWS) {
            l.add(WindowsCredentialManager.class);
        }
        l.add(PasswordManagerCommand.class);
        return l;
    }

    default void checkComplete() throws ValidationException {}

    String getDocsLink();

    String retrievePassword(String key);

    String getKeyPlaceholder();
}
