package io.xpipe.core.data;

import io.xpipe.core.data.type.DataType;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class DataStructureNode implements Iterable<DataStructureNode> {

    protected abstract String getName();

    protected UnsupportedOperationException unuspported(String s) {
        return new UnsupportedOperationException(getName() + " does not support " + s);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public abstract String toString(int indent);

    public boolean isTuple() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public boolean isValue() {
        return false;
    }

    public int size() {
        throw unuspported("size computation");
    }

    public abstract DataType getDataType();

    public DataStructureNode at(int index) {
        throw unuspported("integer indexing");
    }

    public DataStructureNode forKey(String name) {
        throw unuspported("name indexing");
    }

    public Optional<DataStructureNode> forKeyIfPresent(String name) {
        throw unuspported("name indexing");
    }

    public int asInt() {
        throw unuspported("integer conversion");
    }

    public String asString() {
        throw unuspported("string conversion");
    }

    public Stream<DataStructureNode> stream() {
        throw unuspported("stream creation");
    }

    @Override
    public void forEach(Consumer<? super DataStructureNode> action) {
        throw unuspported("for each");
    }

    @Override
    public Spliterator<DataStructureNode> spliterator() {
        throw unuspported("spliterator creation");
    }

    @Override
    public Iterator<DataStructureNode> iterator() {
        throw unuspported("iterator creation");
    }
}
