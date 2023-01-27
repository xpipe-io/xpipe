package io.xpipe.ext.jdbc;

import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.jdbc.address.JdbcAddress;
import io.xpipe.ext.jdbc.auth.AuthMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@AllArgsConstructor
public abstract class JdbcDatabaseServerStore extends JacksonizedValue implements JdbcBaseStore {

    @Builder.Default
    protected final ShellStore proxy = ShellStore.local();

    protected JdbcAddress address;
    protected AuthMethod auth;

    public String getSelectedDatabase() {
        return null;
    }
}
