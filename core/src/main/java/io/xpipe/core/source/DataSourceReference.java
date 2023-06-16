package io.xpipe.core.source;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.Objects;

/**
 * Represents a reference to an XPipe data source.
 * Using {@link DataSourceReference} instances instead of {@link DataSourceId}
 * instances is mainly done for user convenience purposes.
 * <p>
 * While a {@link DataSourceId} represents a unique and canonical identifier for an XPipe data source,
 * there also exist easier and shorter ways to address a data source.
 * This convenience comes at the price of ambiguity and instability for other types of references.
 */
public interface DataSourceReference {

    /**
     * Creates a reference that always refers to the latest data source.
     *
     * @see Latest
     */
    static DataSourceReference latest() {
        return new Latest();
    }

    /**
     * Creates a reference using only the data source name.
     *
     * @see Name
     */
    static DataSourceReference name(String name) {
        return new Name(name);
    }

    /**
     * Convenience method for {@link #id(DataSourceId)}
     *
     * @see DataSourceId#fromString(String)
     */
    static DataSourceReference id(String id) {
        return new Id(DataSourceId.fromString(id));
    }

    /**
     * Creates a reference by using a canonical data source id.
     *
     * @see Id
     */
    static DataSourceReference id(DataSourceId id) {
        return new Id(id);
    }

    static DataSourceReference parse(String s) {
        if (s == null || s.trim().length() == 0) {
            return new Latest();
        }

        if (s.contains(":")) {
            return new Id(DataSourceId.fromString(s));
        }

        return new Name(s.trim());
    }

    Type getType();

    DataSourceId getId();

    String getName();

    /**
     * Returns the internal string representation of this reference.
     */
    String toRefString();

    String toString();

    enum Type {
        ID,
        NAME,
        LATEST
    }

    /**
     * A wrapper class for {@link DataSourceId} instances.
     */
    @Value
    @AllArgsConstructor
    class Id implements DataSourceReference {
        @NonNull
        DataSourceId value;

        @Override
        public String toString() {
            return toRefString();
        }

        @Override
        public String toRefString() {
            return value.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Id id = (Id) o;
            return value.equals(id.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public Type getType() {
            return Type.ID;
        }

        @Override
        public DataSourceId getId() {
            return value;
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Using only the data source name allows for a shorthand way of referring to data sources.
     * This works as long there are no two different data sources with the same name in different collections.
     * If this name reference is ambiguous, the data source referral fails.
     */
    @Value
    @AllArgsConstructor
    class Name implements DataSourceReference {
        @NonNull
        String value;

        @Override
        public String toString() {
            return toRefString();
        }

        @Override
        public String toRefString() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Name n = (Name) o;
            return value.equals(n.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public Type getType() {
            return Type.NAME;
        }

        @Override
        public DataSourceId getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return value;
        }
    }

    /**
     * Specifying the latest reference allows the user to always address the latest data source.
     * Data source referral this way is unstable however as adding or
     * removing data sources might change the referral behaviour and is therefore not recommended.
     */
    class Latest implements DataSourceReference {

        @Override
        public String toRefString() {
            return "";
        }

        @Override
        public String toString() {
            return "latest";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public Type getType() {
            return Type.LATEST;
        }

        @Override
        public DataSourceId getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }
    }
}
