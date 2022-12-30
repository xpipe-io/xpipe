package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DataStructureNode implements Iterable<DataStructureNode> {

    public static final Integer KEY_TABLE_NAME = 1;
    public static final Integer KEY_ROW_NAME = 2;
    public static final Integer BOOLEAN_TRUE = 3;
    public static final Integer IS_BOOLEAN = 4;
    public static final Integer BOOLEAN_FALSE = 5;
    public static final Integer INTEGER_VALUE = 6;
    public static final Integer IS_NULL = 7;
    public static final Integer IS_INTEGER = 9;
    public static final Integer IS_DECIMAL = 10;
    public static final Integer DECIMAL_VALUE = 11;
    public static final Integer IS_TEXT = 12;
    public static final Integer IS_INSTANT = 13;
    public static final Integer IS_BINARY = 14;

    public static final Integer IS_DATE = 15;
    public static final Integer DATE_VALUE = 16;

    public static final Integer IS_CURRENCY = 17;
    public static final Integer CURRENCY_CODE = 18;

    private Map<Integer, String> metaAttributes;

    public void clearMetaAttributes() {
        metaAttributes = null;
        if (isTuple() || isArray()) {
            getNodes().forEach(dataStructureNode -> dataStructureNode.clearMetaAttributes());
        }
    }

    public Map<Integer, String> getMetaAttributes() {
        return metaAttributes != null ? Collections.unmodifiableMap(metaAttributes) : null;
    }

    public DataStructureNode tag(Integer key) {
        if (metaAttributes == null) {
            metaAttributes = new HashMap<>();
        }

        metaAttributes.put(key, null);
        return this;
    }

    public DataStructureNode tag(Map<Integer, String> metaAttributes) {
        if (metaAttributes == null) {
            return this;
        }

        if (this.metaAttributes == null) {
            this.metaAttributes = new HashMap<>();
        }

        this.metaAttributes.putAll(metaAttributes);
        return this;
    }

    public DataStructureNode tag(Integer key, Object value) {
        if (metaAttributes == null) {
            metaAttributes = new HashMap<>();
        }

        metaAttributes.put(key, value.toString());
        return this;
    }

    public String getMetaAttribute(Integer key) {
        if (metaAttributes == null) {
            return null;
        }

        return metaAttributes.get(key);
    }

    public boolean hasMetaAttribute(Integer key) {
        if (metaAttributes == null) {
            return false;
        }

        return metaAttributes.containsKey(key);
    }

    public DataStructureNode mutable() {
        return this;
    }

    public String keyNameAt(int index) {
        throw unsupported("key name at");
    }

    public List<KeyValue> getKeyValuePairs() {
        throw unsupported("get key value pairs");
    }

    public List<String> getKeyNames() {
        throw unsupported("get key names");
    }

    public List<DataStructureNode> getNodes() {
        throw unsupported("get nodes");
    }

    protected abstract String getName();

    protected UnsupportedOperationException unsupported(String s) {
        return new UnsupportedOperationException(getName() + " does not support " + s);
    }

    public abstract boolean isMutable();

    @Override
    public String toString() {
        return toString(0);
    }

    public DataStructureNode clear() {
        throw unsupported("clear");
    }

    public String metaToString() {
        return "("
                + (metaAttributes != null
                ? metaAttributes.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey()))
                .map(e -> e.getValue() != null
                        ? e.getKey() + ":" + e.getValue()
                        : e.getKey().toString())
                .collect(Collectors.joining("|"))
                : "")
                + ")";
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

    public DataStructureNode set(int index, DataStructureNode node) {
        throw unsupported("set at index");
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

    public record KeyValue(String key, DataStructureNode value) {
    }
}
