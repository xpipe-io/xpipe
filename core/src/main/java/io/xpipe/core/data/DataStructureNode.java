package io.xpipe.core.data;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.type.DataType;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class DataStructureNode implements Iterable<DataStructureNode> {

    protected abstract String getName();

    protected UnsupportedOperationException unsupported(String s) {
        return new UnsupportedOperationException(getName() + " does not support " + s);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public DataStructureNode clear() {
        throw unsupported("clear");
    }

    public DataStructureNode setRawData(byte[] data) {
        throw unsupported("set raw data");
    }

    public DataStructureNode setNull() {
        throw unsupported("set null");
    }

    public DataStructureNode set(int index, DataStructureNode node) {
        throw unsupported("set at index");
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

    public boolean isNull() {
        throw unsupported("null check");
    }

    public final ValueNode asValue() {
        if (!isValue()) {
            throw new UnsupportedOperationException(getName() + " is not a value node");
        }

        return (ValueNode) this;
    }

    public final TupleNode asTuple() {
        if (!isTuple()) {
            throw new UnsupportedOperationException(getName() + " is not a tuple node");
        }

        return (TupleNode) this;
    }

    public final ArrayNode asArray() {
        if (!isArray()) {
            throw new UnsupportedOperationException(getName() + " is not an array node");
        }

        return (ArrayNode) this;
    }

    public DataStructureNode put(String keyName, DataStructureNode node) {
        throw unsupported("put node with key");
    }

    public DataStructureNode put(DataStructureNode node) {
        throw unsupported("put node");
    }

    public DataStructureNode remove(int index) {
        throw unsupported("index remove");
    }

    public DataStructureNode remove(String keyName) {
        throw unsupported("key remove");
    }

    public int size() {
        throw unsupported("size computation");
    }

    public abstract DataType determineDataType();

    public DataStructureNode at(int index) {
        throw unsupported("integer indexing");
    }

    public DataStructureNode forKey(String name) {
        throw unsupported("name indexing");
    }

    public Optional<DataStructureNode> forKeyIfPresent(String name) {
        throw unsupported("name indexing");
    }

    public int asInt() {
        throw unsupported("integer conversion");
    }

    public String asString() {
        throw unsupported("string conversion");
    }

    public Stream<DataStructureNode> stream() {
        throw unsupported("stream creation");
    }

    @Override
    public void forEach(Consumer<? super DataStructureNode> action) {
        throw unsupported("for each");
    }

    @Override
    public Spliterator<DataStructureNode> spliterator() {
        throw unsupported("spliterator creation");
    }

    @Override
    public Iterator<DataStructureNode> iterator() {
        throw unsupported("iterator creation");
    }
}
