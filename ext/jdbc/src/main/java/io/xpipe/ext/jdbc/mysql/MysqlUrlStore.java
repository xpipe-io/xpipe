package io.xpipe.ext.jdbc.mysql;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.ShellStore;
import io.xpipe.ext.jdbc.JdbcBaseStore;
import io.xpipe.ext.jdbc.JdbcUrlStore;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("mysqlUrl")
@SuperBuilder
@Jacksonized
@Getter
public class MysqlUrlStore extends JdbcUrlStore implements JdbcBaseStore {

    @Builder.Default
    protected ShellStore proxy = ShellStore.local();

    public MysqlUrlStore(ShellStore proxy, String url) {
        super(url);
        this.proxy = proxy;
    }

    @Override
    public String getAddress() {
        return getUrl().substring(0, getUrl().indexOf("/"));
    }

    @Override
    protected String getProtocol() {
        return MysqlStoreProvider.PROTOCOL;
    }
}
