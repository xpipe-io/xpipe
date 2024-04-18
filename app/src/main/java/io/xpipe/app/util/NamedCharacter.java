package io.xpipe.app.util;

import io.xpipe.core.dialog.QueryConverter;

import lombok.Value;

import java.util.List;

@Value
public class NamedCharacter {

    char character;
    List<String> names;
    String translationKey;

    public static QueryConverter<Character> converter(List<NamedCharacter> chars, boolean allowOthers) {
        return new QueryConverter<>() {
            @Override
            protected Character fromString(String s) {
                if (s.length() == 0) {
                    throw new IllegalArgumentException("No character");
                }

                var byName = chars.stream()
                        .filter(nc -> nc.getNames().stream()
                                .anyMatch(n -> n.toLowerCase().contains(s.toLowerCase())))
                        .findFirst()
                        .orElse(null);
                if (byName != null) {
                    return byName.getCharacter();
                }

                var byChar = chars.stream()
                        .filter(nc -> String.valueOf(nc.getCharacter()).equalsIgnoreCase(s))
                        .findFirst()
                        .orElse(null);
                if (byChar != null) {
                    return byChar.getCharacter();
                }

                if (!allowOthers) {
                    throw new IllegalArgumentException("Unknown character: " + s);
                }

                return QueryConverter.CHARACTER.convertFromString(s);
            }

            @Override
            protected String toString(Character value) {
                var byChar = chars.stream()
                        .filter(nc -> value.equals(nc.getCharacter()))
                        .findFirst()
                        .orElse(null);
                if (byChar != null) {
                    return byChar.getNames().getFirst();
                }

                return value.toString();
            }
        };
    }
}
