package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.InternalCacheDataStore;
import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.app.ext.ValidatableStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.identity.ssh.NoneStrategy;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;

@SuperBuilder
@JsonTypeName("passwordManagerIdentity")
@Jacksonized
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PasswordManagerIdentityStore extends IdentityStore
        implements InternalCacheDataStore, ValidatableStore, UserScopeStore {

    String key;
    boolean perUser;

    private boolean checkOutdatedOrRefresh() {
        var instant = getCache("lastQueried", Instant.class, null);
        if (instant != null) {
            var now = Instant.now();
            if (Duration.between(instant, now).toSeconds() < 15) {
                return false;
            }
        }

        return true;
    }

    private PasswordManager.CredentialResult retrieveCredentials() {
        if (!checkOutdatedOrRefresh()) {
            var credential = getCache("credential", PasswordManager.CredentialResult.class, null);
            if (credential != null) {
                return credential;
            }
        }

        if (AppPrefs.get() == null || AppPrefs.get().passwordManager().getValue() == null) {
            return null;
        }

        var r = AppPrefs.get().passwordManager().getValue().retrieveCredentials(key);
        if (r == null) {
            throw ErrorEventFactory.expected(
                    new UnsupportedOperationException("Credentials were requested but not supplied"));
        }

        if (r.getUsername() == null) {
            throw ErrorEventFactory.expected(
                    new UnsupportedOperationException("Identity " + key + " does not provide a username"));
        }

        if (r.getPassword() == null) {
            throw ErrorEventFactory.expected(
                    new UnsupportedOperationException("Identity " + key + " does not provide a password"));
        }

        setCache("lastQueried", Instant.now());
        setCache("credential", r);

        return r;
    }

    public UsernameStrategy getUsername() {
        return new UsernameStrategy.Dynamic(() -> {
            var r = retrieveCredentials();
            return r.getUsername();
        });
    }

    @Override
    public SecretRetrievalStrategy getPassword() {
        return new SecretRetrievalStrategy() {

            @Override
            public SecretQuery query() {
                return new SecretQuery() {
                    @Override
                    public SecretQueryResult query(String prompt) {
                        var r = retrieveCredentials();
                        return new SecretQueryResult(r.getPassword(), SecretQueryState.NORMAL);
                    }

                    @Override
                    public Duration cacheDuration() {
                        return null;
                    }

                    @Override
                    public boolean retryOnFail() {
                        return false;
                    }

                    @Override
                    public boolean requiresUserInteraction() {
                        return false;
                    }
                };
            }
        };
    }

    @Override
    public SshIdentityStrategy getSshIdentity() {
        return new NoneStrategy();
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(key);
    }

    @Override
    public void validate() {
        retrieveCredentials();
    }
}
