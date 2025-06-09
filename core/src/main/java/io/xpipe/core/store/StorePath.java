package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a reference to an XPipe storage location.
 * <p>
 * To allow for a simple usage, the names are trimmed and
 * converted to lower case names when creating them.
 * The names are separated by a slash and are therefore not allowed to contain slashes themselves.
 *
 * @see #fromString(String)
 */
@EqualsAndHashCode
@Getter
public class StorePath {

    public static final char SEPARATOR = '/';

    private final List<String> names;

    @JsonCreator
    public StorePath(List<String> names) {
        this.names = names;
    }

    /**
     * Creates a new store path.
     *
     * @throws IllegalArgumentException if any name is not valid
     */
    public static StorePath create(String... names) {
        if (names == null) {
            throw new IllegalArgumentException("Names are null");
        }

        if (Arrays.stream(names).anyMatch(s -> s == null)) {
            throw new IllegalArgumentException("Name is null");
        }

        if (Arrays.stream(names).anyMatch(s -> s.contains("" + SEPARATOR))) {
            throw new IllegalArgumentException("Separator character " + SEPARATOR + " is not allowed in the names");
        }

        if (Arrays.stream(names).anyMatch(s -> s.strip().length() == 0)) {
            throw new IllegalArgumentException("Trimmed entry name is empty");
        }

        return new StorePath(Arrays.stream(names).toList());
    }

    /**
     * Creates a new store path from a string representation.
     *
     * @param s the string representation, must be not null and fulfill certain requirements
     * @throws IllegalArgumentException if the string is not valid
     */
    public static StorePath fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("String is null");
        }

        var split = s.split(String.valueOf(SEPARATOR), -1);

        var names =
                Arrays.stream(split).map(String::trim).map(String::toLowerCase).toList();
        if (names.stream().anyMatch(s1 -> s1.isEmpty())) {
            throw new IllegalArgumentException("Name must not be empty");
        }

        return new StorePath(names);
    }

    @Override
    public String toString() {
        return names.stream().map(String::toLowerCase).collect(Collectors.joining("" + SEPARATOR));
    }
}
