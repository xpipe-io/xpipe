package io.xpipe.ext.jdbcx.mssql;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.ShellStore;
import io.xpipe.ext.jdbc.JdbcUrlStore;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("mssqlUrl")
@SuperBuilder
@Jacksonized
@Getter
public class MssqlUrlStore extends JdbcUrlStore implements MssqlStore {

    @Builder.Default
    protected ShellStore proxy = ShellStore.local();

    public MssqlUrlStore(ShellStore proxy, String url) {
        super(url);
        this.proxy = proxy;
    }

    @Override
    public String getAddress() {
        return getUrl().substring(0, getUrl().indexOf(";"));
    }

    @Override
    protected String getProtocol() {
        return MssqlStoreProvider.PROTOCOL;
    }
}
