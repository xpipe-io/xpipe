package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a reference to an XPipe data source.
 * This reference consists out of a collection name and an entry name to allow for better organisation.
 * <p>
 * To allow for a simple usage of data source ids, the collection and entry names are trimmed and
 * converted to lower case names when creating them.
 * The two names are separated by a colon and are therefore not allowed to contain colons themselves.
 * <p>
 * A missing collection name indicates that the data source exists only temporarily.
 *
 * @see #fromString(String)
 */
@EqualsAndHashCode
@Getter
public class DataStoreId {

    public static final char SEPARATOR = ':';

    private final List<String> names;

    @JsonCreator
    public DataStoreId(List<String> names) {
        this.names = names;
    }

    /**
     * Creates a new data source id from a collection name and an entry name.
     *
     * @throws IllegalArgumentException if any name is not valid
     */
    public static DataStoreId create(String... names) {
        if (names == null) {
            throw new IllegalArgumentException("Names are null");
        }

        if (Arrays.stream(names).anyMatch(s -> s.contains("" + SEPARATOR))) {
            throw new IllegalArgumentException(
                    "Separator character " + SEPARATOR + " is not allowed in the names");
        }

        if (Arrays.stream(names).anyMatch(s -> s.trim().length() == 0)) {
            throw new IllegalArgumentException("Trimmed entry name is empty");
        }

        return new DataStoreId(Arrays.stream(names).toList());
    }

    /**
     * Creates a new data source id from a string representation.
     * The string must contain exactly one colon and non-empty names.
     *
     * @param s the string representation, must be not null and fulfill certain requirements
     * @throws IllegalArgumentException if the string is not valid
     */
    public static DataStoreId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("String is null");
        }

        var split = s.split(String.valueOf(SEPARATOR));

        var names = Arrays.stream(split).toList();
        if (names.stream().anyMatch(s1 -> s1.isEmpty())) {
            throw new IllegalArgumentException("Name must not be empty");
        }

        return new DataStoreId(names);
    }

    @Override
    public String toString() {
        return names.stream().map(String::toLowerCase).collect(Collectors.joining("" + SEPARATOR));
    }
}
