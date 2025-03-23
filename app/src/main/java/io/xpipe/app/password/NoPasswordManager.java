package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.OptionsBuilder;
import javafx.beans.property.Property;
import lombok.Value;

@JsonTypeName("none")
@Value
public class NoPasswordManager implements PasswordManager {

    static OptionsBuilder createOptions(Property<NoPasswordManager> property) {
        return new OptionsBuilder().bind(() -> new NoPasswordManager(), property);
    }

    @Override
    public String getDocsLink() {
        return null;
    }

    @Override
    public String retrievePassword(String key) {
        throw ErrorEvent.expected(new UnsupportedOperationException("No password manager has been configured"));
    }

    @Override
    public String getKeyPlaceholder() {
        return "?";
    }
}
