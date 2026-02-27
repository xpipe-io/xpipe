package io.xpipe.ext.base.identity;

import io.xpipe.app.cred.InPlaceKeyStrategy;
import io.xpipe.app.cred.UsernameStrategy;
import io.xpipe.app.ext.InternalCacheDataStore;
import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.app.ext.ValidatableStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.secret.*;
import io.xpipe.app.util.*;
import io.xpipe.app.cred.NoIdentityStrategy;
import io.xpipe.app.cred.SshIdentityStrategy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.KeyValue;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
            var pm = AppPrefs.get().passwordManager().getValue();
            var cacheDuration = pm != null ? pm.getCacheDuration().toSeconds() : 15;
            if (Duration.between(instant, now).toSeconds() < cacheDuration) {
                return false;
            }
        }

        return true;
    }

    private PasswordManager.Result retrieveCredentials() {
        if (!checkOutdatedOrRefresh()) {
            var r = getCache("result", PasswordManager.Result.class, null);
            if (r != null) {
                return r;
            }
        }

        if (AppPrefs.get() == null || AppPrefs.get().passwordManager().getValue() == null) {
            return null;
        }

        var r = AppPrefs.get().passwordManager().getValue().query(key);
        if (r == null) {
            throw ErrorEventFactory.expected(
                    new UnsupportedOperationException("Credentials were requested but not supplied"));
        }

        if (r.getCredentials() == null) {
            throw ErrorEventFactory.expected(
                    new UnsupportedOperationException("Identity " + key + " does not provide credentials"));
        }

        if (r.getCredentials().getUsername() == null) {
            throw ErrorEventFactory.expected(
                    new UnsupportedOperationException("Identity " + key + " does not provide a username"));
        }

        setCache("lastQueried", Instant.now());
        setCache("result", r);

        return r;
    }

    public UsernameStrategy getUsername() {
        return new UsernameStrategy.Dynamic(() -> {
            var r = retrieveCredentials();
            var effective = r != null && r.getCredentials() != null && r.getCredentials().getUsername() != null ? r.getCredentials().getUsername() : "unknown";
            return effective;
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
                        if (r == null || r.getCredentials() == null || r.getCredentials().getPassword() == null) {
                            return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                        }

                        return new SecretQueryResult(r.getCredentials().getPassword(), SecretQueryState.NORMAL);
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
        var def = new NoIdentityStrategy();
        var r = AppPrefs.get().passwordManager().getValue();
        if (r == null) {
            return def;
        }

        var strat = r.getKeyStrategy();
        if (strat == null || (!strat.supportsInlineSshKeys() && !strat.supportsAgent())) {
            return def;
        }

        if (strat.supportsInlineSshKeys()) {
            return new SshIdentityStrategy() {
                @Override
                public void prepareParent(ShellControl parent) throws Exception {
                    var r = retrieveCredentials();
                    if (r == null || r.getSshKey() == null || r.getSshKey().getPrivateKey() == null) {
                        return;
                    }

                    var inPlace = new InPlaceKeyStrategy(r.getSshKey().getPrivateKey(), null, new SecretPromptStrategy());
                    inPlace.prepareParent(parent);
                }

                @Override
                public void buildCommand(CommandBuilder builder) {
                    var r = retrieveCredentials();
                    if (r == null || r.getSshKey() == null || r.getSshKey().getPrivateKey() == null) {
                        return;
                    }

                    var inPlace = new InPlaceKeyStrategy(r.getSshKey().getPrivateKey(), null, new SecretPromptStrategy());
                    inPlace.buildCommand(builder);
                }

                @Override
                public List<KeyValue> configOptions(ShellControl sc) throws Exception {
                    var r = retrieveCredentials();
                    if (r == null || r.getSshKey() == null || r.getSshKey().getPrivateKey() == null) {
                        return List.of();
                    }

                    var inPlace = new InPlaceKeyStrategy(r.getSshKey().getPrivateKey(), null, new SecretPromptStrategy());
                    return inPlace.configOptions(sc);
                }

                @Override
                public String getPublicKey() {
                    var r = retrieveCredentials();
                    if (r == null || r.getSshKey() == null || r.getSshKey().getPublicKey() == null) {
                        return null;
                    }

                    return r.getSshKey().getPublicKey();
                }
            };
        }

        var agentStrat = strat.getSshIdentityStrategy();
        return new NoIdentityStrategy();
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
