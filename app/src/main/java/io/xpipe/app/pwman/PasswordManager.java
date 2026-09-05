package io.xpipe.app.pwman;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.AuthModuleProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.PrefsCapabilities;
import io.xpipe.app.prefs.PrefsCapability;
import io.xpipe.app.prefs.PrefsCapabilityProvider;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.InPlaceSecretValue;
import io.xpipe.app.util.SecretValue;
import io.xpipe.app.webtop.WebtopApp;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface PasswordManager extends PrefsCapabilityProvider {

    @Override
    default PrefsCapabilities getCapabilities() {
        var listing = supportsList();
        var keys = supportsKeyConfiguration();
        return PrefsCapabilities.of(
                PrefsCapability.of("pwmanCapabilityListing", PrefsCapability.Type.of(listing)),
                PrefsCapability.of("pwmanCapabilitySshKeys", PrefsCapability.Type.of(keys))
        );
    }

    default PasswordManager validated() {
        return this;
    }

    default boolean supportsList() {
        return false;
    }

    default List<ListEntry> listKeys() {
        return List.of();
    }

    default WebtopApp getRequiredWebtopApp() {
        return null;
    }

    default String getDisplayName() {
        var a = getClass().getAnnotation(JsonTypeName.class);
        if (a != null) {
            return AppI18n.get(a.value());
        } else {
            return "?";
        }
    }

    @SneakyThrows
    static PasswordManager determineDefault(PasswordManager existing) {
        if (existing != null) {
            return existing;
        }

        if (!AppProperties.get().isInitialLaunch()) {
            return null;
        }

        try {
            for (Class<?> c : AuthModuleProvider.get().getPasswordManagerClasses()) {
                var bm = c.getDeclaredMethod("builder");
                bm.setAccessible(true);
                var b = bm.invoke(null);

                var m = b.getClass().getDeclaredMethod("build");
                m.setAccessible(true);
                var defValue = (PasswordManager) c.cast(m.invoke(b));
                if (defValue.selectInitial()) {
                    return defValue;
                }
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
        return null;
    }

    Result query(String key);

    String getKeyPlaceholder();

    String getWebsite();

    boolean supportsKeyConfiguration();

    PasswordManagerKeyConfiguration getKeyConfiguration();

    boolean selectInitial() throws Exception;

    default Duration getCacheDuration() {
        return Duration.ofSeconds(30);
    }

    default ShellControl getShell() throws Exception {
        return LocalShell.getInstance(getClass());
    }

    enum ListEntryType {
        UNKNOWN,
        LOGIN,
        KEY,
        BOTH
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor
    class ListEntry {

        String title;
        String key;
        String internalId;
        ListEntryType type;
        List<String> urls;

        public boolean matches(String filter) {
            return title.toLowerCase().contains(filter.toLowerCase())
                    || key.toLowerCase().contains(filter.toLowerCase())
                    || urls.stream().anyMatch(url -> url.toLowerCase().contains(filter.toLowerCase()))
                    || (internalId != null && internalId.equalsIgnoreCase(filter));
        }
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Result {

        public static Result of(Credentials creds, SshKey sshKey) {
            if (creds == null && sshKey == null) {
                return null;
            }
            return new Result(creds, sshKey);
        }

        Credentials credentials;
        SshKey sshKey;
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SshKey {

        public static SshKey of(String publicKey, String privateKey) {
            if (publicKey == null && privateKey == null) {
                return null;
            }

            return new SshKey(publicKey, privateKey != null ? InPlaceSecretValue.of(privateKey) : null);
        }

        String publicKey;
        SecretValue privateKey;
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Credentials {

        public static Credentials of(String username, String password) {
            if (username == null && password == null) {
                return null;
            }

            return new Credentials(
                    username != null && !username.isEmpty() ? username : null,
                    password != null && !password.isEmpty() ? InPlaceSecretValue.of(password) : null);
        }

        String username;
        SecretValue password;
    }
}
