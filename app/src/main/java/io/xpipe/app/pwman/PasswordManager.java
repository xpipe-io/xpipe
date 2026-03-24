package io.xpipe.app.pwman;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.OsType;
import io.xpipe.core.SecretValue;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface PasswordManager {

    @SneakyThrows
    static PasswordManager determineDefault(PasswordManager existing) {
        if (existing != null) {
            return existing;
        }

        if (!AppProperties.get().isInitialLaunch()) {
            return null;
        }

        try {
            for (Class<?> c : PasswordManager.getClasses()) {
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

    @SneakyThrows
    static boolean isPasswordManagerSshAgent(String s) {
        for (Class<?> c : PasswordManager.getClasses()) {
            var bm = c.getDeclaredMethod("builder");
            bm.setAccessible(true);
            var b = bm.invoke(null);

            var m = b.getClass().getDeclaredMethod("build");
            m.setAccessible(true);
            var defValue = (PasswordManager) c.cast(m.invoke(b));
            var config = defValue.getKeyConfiguration();
            if (config.getDefaultSocketLocation() != null
                    && config.getDefaultSocketLocation().toString().equals(s)) {
                return true;
            }
        }
        return false;
    }

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(OnePasswordManager.class);
        l.add(KeePassXcPasswordManager.class);
        l.add(BitwardenPasswordManager.class);
        l.add(KeeperPasswordManager.class);
        l.add(ProtonPasswordManager.class);
        l.add(HashicorpVaultPasswordManager.class);
        if (OsType.ofLocal() != OsType.WINDOWS) {
            l.add(LastpassPasswordManager.class);
            l.add(EnpassPasswordManager.class);
        }
        l.add(DashlanePasswordManager.class);
        l.add(PsonoPasswordManager.class);
        l.add(PassboltPasswordManager.class);
        if (OsType.ofLocal() == OsType.WINDOWS) {
            l.add(WindowsCredentialManager.class);
        }
        l.add(PasswordManagerCommand.class);
        return l;
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
