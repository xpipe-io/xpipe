package io.xpipe.app.pwman;

import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.OsType;
import io.xpipe.core.SecretValue;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface PasswordManager {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(OnePasswordManager.class);
        l.add(KeePassXcPasswordManager.class);
        l.add(BitwardenPasswordManager.class);
        l.add(DashlanePasswordManager.class);
        if (OsType.ofLocal() != OsType.WINDOWS) {
            l.add(LastpassPasswordManager.class);
            l.add(EnpassPasswordManager.class);
        }
        l.add(KeeperPasswordManager.class);
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

    PasswordManagerKeyStrategy getKeyStrategy();

    default Duration getCacheDuration() {
        return Duration.ofSeconds(30);
    }

    @Value
    class Result {

        Credentials credentials;
        SshKey sshKey;
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SshKey {

        public static SshKey of(String fingerprint, String publicKey, String privateKey) {
            if (fingerprint == null && publicKey == null && privateKey == null) {
                return null;
            }

            return new SshKey(fingerprint, publicKey, privateKey != null ? InPlaceSecretValue.of(privateKey) : null);
        }

        String fingerprint;
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

            return new Credentials(username != null && !username.isEmpty() ? username : null,
                    password != null && !password.isEmpty() ? InPlaceSecretValue.of(password) : null);
        }

        String username;
        SecretValue password;
    }
}
