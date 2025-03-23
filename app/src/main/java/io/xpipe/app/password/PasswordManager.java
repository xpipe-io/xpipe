package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.*;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.ValidationException;
import javafx.beans.property.Property;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PasswordManager.None.class),
    @JsonSubTypes.Type(value = WindowsCredentialManager.class),
    @JsonSubTypes.Type(value = KeePassXcManager.class)
})
public interface PasswordManager {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(PasswordManager.None.class);
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

    @JsonTypeName("none")
    @Value
    class None implements PasswordManager {

        static OptionsBuilder createOptions(Property<None> property) {
            return new OptionsBuilder().bind(() -> new None(), property);
        }

        @Override
        public String getDocsLink() {
            return null;
        }

        @Override
        public String retrievePassword(String key) {
            throw ErrorEvent.expected(new UnsupportedOperationException("No password manager has been configured"));
        }
    }

}
