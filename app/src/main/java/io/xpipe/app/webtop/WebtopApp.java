package io.xpipe.app.webtop;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum WebtopApp {

    ONE_PASSWORD("1password");

    public static Optional<WebtopApp> fromString(String value) {
        return Arrays.stream(WebtopApp.values())
                .filter(webtopApp -> webtopApp.getId().equals(value))
                .findFirst();
    }

    private final String translationKey;
    private final String id;

    WebtopApp(String translationKey) {
        this(translationKey, translationKey);
    }

    WebtopApp(String translationKey, String id) {
        this.translationKey = translationKey;
        this.id = id;
    }
}
