package io.xpipe.ext.jdbcx.oracle;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.ShellStore;
import io.xpipe.ext.jdbc.JdbcBaseStore;
import io.xpipe.ext.jdbc.JdbcUrlStore;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("oracleUrl")
@SuperBuilder
@Jacksonized
@Getter
public class OracleUrlStore extends JdbcUrlStore implements JdbcBaseStore {

    @Builder.Default
    protected ShellStore proxy = ShellStore.local();

    public OracleUrlStore(ShellStore proxy, String url) {
        super(url);
        this.proxy = proxy;
    }

    @Override
    public String getAddress() {
        return getUrl().substring(0, getUrl().indexOf("/"));
    }

    @Override
    protected String getProtocol() {
        return OracleStoreProvider.PROTOCOL;
    }
}
