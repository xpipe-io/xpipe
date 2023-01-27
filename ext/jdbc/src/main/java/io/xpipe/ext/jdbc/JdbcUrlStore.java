package io.xpipe.ext.jdbc;

import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.core.util.SecretValue;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@Getter
public abstract class JdbcUrlStore extends JacksonizedValue implements JdbcBaseStore {

    private final SecretValue url;

    public JdbcUrlStore(String url) {
        this.url = SecretValue.encrypt(url);
    }

    public String getUrl() {
        return url.getSecretValue();
    }

    @Override
    public String toUrl() {
        return "jdbc:" + getProtocol() + "://" + getUrl();
    }

    public abstract String getAddress();

    protected abstract String getProtocol();

    @Override
    public Map<String, String> createProperties() {
        return Map.of();
    }
}
