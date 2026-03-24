package io.xpipe.ext.base.identity;

import io.xpipe.app.cred.*;
import io.xpipe.app.ext.InternalCacheDataStore;
import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.app.ext.ValidatableStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.secret.*;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.core.KeyValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
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
    PasswordManagerAgentStrategy sshKey;
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

    @SneakyThrows
    private PasswordManager.Result retrieve() {
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
            throw ErrorEventFactory.expected(new UnsupportedOperationException("Credentials for input " + key
                    + " were requested but could not be supplied by the password manager"));
        }

        if (r.getSshKey() != null && r.getCredentials() == null) {
            throw ErrorEventFactory.expected(
                    new UnsupportedOperationException(
                            "Identity " + key
                                    + " does not provide credentials, only a key. Use another credentials entry as a base and reference the key via the password manager agent option instead"));
        }

        if (r.getCredentials() == null) {
            throw ErrorEventFactory.expected(
                    new UnsupportedOperationException("Identity " + key + " does not provide credentials"));
        }

        if (r.getCredentials().getUsername() == null) {
            throw ErrorEventFactory.expected(
                    new UnsupportedOperationException("Identity " + key + " does not provide a username"));
        }

        if (sshKey != null) {
            var pwman = AppPrefs.get().passwordManager().getValue();
            if (pwman.getKeyConfiguration().useInline() && r.getSshKey() == null) {
                throw ErrorEventFactory.expected(
                        new UnsupportedOperationException("Identity " + key + " does not provide an SSH key"));
            }

            if (pwman.getKeyConfiguration().useAgent()) {
                SshAgentKeyList.findAgentIdentity(
                        DataStorage.get().local().ref(),
                        pwman.getKeyConfiguration().getSshIdentityStrategy(null, false),
                        sshKey.getIdentifier());
            }
        }

        setCache("lastQueried", Instant.now());
        setCache("result", r);

        return r;
    }

    public UsernameStrategy getUsername() {
        return new UsernameStrategy.Dynamic(() -> {
            var r = retrieve();
            var effective = r != null
                            && r.getCredentials() != null
                            && r.getCredentials().getUsername() != null
                    ? r.getCredentials().getUsername()
                    : "unknown";
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
                    public SecretQueryResult query(String prompt, boolean forceFocus) {
                        var r = retrieve();
                        if (r == null
                                || r.getCredentials() == null
                                || r.getCredentials().getPassword() == null) {
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

    public boolean hasAgentKey() {
        var r = AppPrefs.get().passwordManager().getValue();
        if (r == null) {
            return false;
        }

        var strat = r.getKeyConfiguration();
        if (strat == null || (!strat.useInline() && !strat.useAgent())) {
            return false;
        }

        return strat.useAgent() && sshKey != null;
    }

    @Override
    public SshIdentityStrategy getSshIdentity() {
        var def = new NoIdentityStrategy();
        var r = AppPrefs.get().passwordManager().getValue();
        if (r == null) {
            return def;
        }

        var strat = r.getKeyConfiguration();
        if (strat == null || (!strat.useInline() && !strat.useAgent())) {
            return def;
        }

        if (strat.useInline()) {
            return new SshIdentityStrategy() {
                @Override
                public void prepareParent(ShellControl parent) throws Exception {
                    var r = retrieve();
                    if (r == null || r.getSshKey() == null || r.getSshKey().getPrivateKey() == null) {
                        return;
                    }

                    var inPlace =
                            new InPlaceKeyStrategy(r.getSshKey().getPrivateKey(), null, new SecretPromptStrategy());
                    inPlace.prepareParent(parent);
                }

                @Override
                public void buildCommand(CommandBuilder builder) {
                    var r = retrieve();
                    if (r == null || r.getSshKey() == null || r.getSshKey().getPrivateKey() == null) {
                        return;
                    }

                    var inPlace =
                            new InPlaceKeyStrategy(r.getSshKey().getPrivateKey(), null, new SecretPromptStrategy());
                    inPlace.buildCommand(builder);
                }

                @Override
                public List<KeyValue> configOptions(ShellControl sc) {
                    var r = retrieve();
                    if (r == null || r.getSshKey() == null || r.getSshKey().getPrivateKey() == null) {
                        return List.of();
                    }

                    var inPlace =
                            new InPlaceKeyStrategy(r.getSshKey().getPrivateKey(), null, new SecretPromptStrategy());
                    return inPlace.configOptions(sc);
                }

                @Override
                public PublicKeyStrategy getPublicKeyStrategy() {
                    var r = retrieve();
                    if (r == null || r.getSshKey() == null || r.getSshKey().getPublicKey() == null) {
                        return null;
                    }

                    return PublicKeyStrategy.Fixed.of(r.getSshKey().getPublicKey());
                }
            };
        }

        if (strat.useAgent() && sshKey != null) {
            return sshKey;
        }

        return new NoIdentityStrategy();
    }

    @Override
    public List<DataStoreEntryRef<?>> getDependencies() {
        return List.of();
    }

    @Override
    public void checkComplete() throws ValidationException {
        Validators.nonNull(key);
        if (sshKey != null) {
            sshKey.checkComplete();

            var pwman = AppPrefs.get().passwordManager().getValue();
            if (pwman != null) {
                var keyConfig = pwman.getKeyConfiguration();
                if (keyConfig != null && !keyConfig.useAgent()) {
                    throw new ValidationException("Password manager is not configured to use an agent");
                }
            }
        }
    }

    @Override
    public void validate() {
        retrieve();
    }
}
