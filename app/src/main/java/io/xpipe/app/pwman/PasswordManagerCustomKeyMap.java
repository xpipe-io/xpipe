package io.xpipe.app.pwman;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.stream.Collectors;

@Value
@Jacksonized
@Builder
@AllArgsConstructor
public class PasswordManagerCustomKeyMap {

    public static PasswordManagerCustomKeyMap overlay(
            PasswordManagerCustomKeyMap global, PasswordManagerCustomKeyMap override) {
        return new PasswordManagerCustomKeyMap(
                override.user != null ? override.user : global.user,
                override.password != null ? override.password : global.password,
                override.publicKey != null ? override.publicKey : global.publicKey,
                override.privateKey != null ? override.privateKey : global.privateKey);
    }

    public static PasswordManagerCustomKeyMap fromInput(String key) {
        var keySplit = key.split("\\?", 2);
        var keys = Arrays.stream((keySplit.length > 1 ? keySplit[1] : "").split("&"))
                .filter(s -> s.split("=").length == 2)
                .collect(Collectors.toMap(s -> s.split("=", 2)[0], s -> s.split("=", 2)[1]));
        return new PasswordManagerCustomKeyMap(
                keys.get("user"), keys.get("pass"), keys.get("public-key"), keys.get("private-key"));
    }

    String user;
    String password;
    String publicKey;
    String privateKey;

    public boolean isEmpty() {
        return user == null && password == null && publicKey == null && privateKey == null;
    }
}
