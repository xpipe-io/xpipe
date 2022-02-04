package io.xpipe.core.source;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.Objects;

public interface DataSourceReference {

    static DataSourceReference empty() {
        return new Empty();
    }

    public static DataSourceReference parse(String s) {
        if (s == null || s.trim().length() == 0) {
            return new Empty();
        }

        if (s.contains(":")) {
            return new Id(DataSourceId.fromString(s));
        }

        return new Name(s.trim());
    }

    enum Type {
        ID,
        NAME,
        EMPTY
    }

    Type getType();
    DataSourceId getId();
    String getName();
    String toRefString();
    String toString();

    @Value
    @AllArgsConstructor
    static class Id implements DataSourceReference {
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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

    @Value
    @AllArgsConstructor
    static class Name implements DataSourceReference {
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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

    static class Empty implements DataSourceReference {

        @Override
        public String toRefString() {
            return "";
        }

        @Override
        public String toString() {
            return "none";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public Type getType() {
            return Type.EMPTY;
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
