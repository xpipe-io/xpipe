package io.xpipe.ext.pdx.parser;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.DataType;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ContextTupleNode extends TupleNode {

    private final NodeContext context;
    private final int[] keyScalars;
    private final int[] valueScalars;
    private final List<DataStructureNode> values;

    ContextTupleNode(NodeContext context, int[] keyScalars, int[] valueScalars, List<DataStructureNode> values) {
        this.context = Objects.requireNonNull(context);
        this.keyScalars = keyScalars;
        this.valueScalars = valueScalars;
        this.values = Objects.requireNonNull(values);
    }

    @Override
    public String toString() {
        if (values.size() == 0) {
            return "SimpleArrayNode(0)";
        }

        if (values.size() <= 10) {
            StringBuilder sb = new StringBuilder("SimpleArrayNode(");
            evaluateAllValueNodes();
            for (int i = 0; i < values.size(); i++) {
                if (hasKeyAtIndex(i)) {
                    sb.append(context.evaluate(keyScalars[i]));
                    sb.append("=");
                }
                sb.append(values.get(i).toString());
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append(")");
            return sb.toString();
        } else {
            return "SimpleArrayNode(" + values.size() + ")";
        }
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public DataType determineDataType() {
        return null;
    }

    private void evaluateNodeAt(int i) {
        if (values.get(i) == null) {
            values.set(i, new ContextValueNode(context, valueScalars[i]));
        }
    }

    private void evaluateAllValueNodes() {
        for (int i = 0; i < values.size(); i++) {
            evaluateNodeAt(i);
        }
    }

    private boolean hasKeyAtIndex(int index) {
        if (keyScalars == null) {
            return false;
        }

        return keyScalars[index] != -1;
    }

    private boolean isKeyAt(int index, byte[] b) {
        if (!hasKeyAtIndex(index)) {
            return false;
        }

        int keyScalarIndex = keyScalars[index];
        if (context.getLiteralsLength()[keyScalarIndex] != b.length) {
            return false;
        }

        int start = context.getLiteralsBegin()[keyScalarIndex];
        for (int i = 0; i < context.getLiteralsLength()[keyScalarIndex]; i++) {
            if (context.getData()[start + i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private DataStructureNode getNodeForKeyInternal(String key) {
        // Check if this node has no keys
        if (keyScalars == null) {
            return null;
        }

        var b = key.getBytes(context.getCharset());
        for (int i = 0; i < values.size(); i++) {
            if (isKeyAt(i, b)) {
                // Initialize value node if we haven't done that already
                evaluateNodeAt(i);
                return values.get(i);
            }
        }

        return null;
    }

    @Override
    public String keyNameAt(int index) {
        if (!hasKeyAtIndex(index)) {
            return null;
        }

        return context.evaluate(keyScalars[index]);
    }

    @Override
    public List<DataStructureNode> getNodes() {
        evaluateAllValueNodes();
        return Collections.unmodifiableList(values);
    }

    @Override
    public DataStructureNode at(int index) {
        return getNodes().get(index);
    }

    @Override
    public DataStructureNode forKey(String name) {
        var n = getNodeForKeyInternal(name);
        if (n != null) {
            return n;
        }

        throw new IllegalArgumentException("Invalid key " + name);
    }

    @Override
    public Optional<DataStructureNode> forKeyIfPresent(String name) {
        return Optional.ofNullable(getNodeForKeyInternal(name));
    }

    @Override
    public Stream<DataStructureNode> stream() {
        return getNodes().stream();
    }

    @Override
    public void forEach(Consumer<? super DataStructureNode> action) {
        stream().forEach(action);
    }

    @Override
    public Spliterator<DataStructureNode> spliterator() {
        return stream().spliterator();
    }

    @Override
    public Iterator<DataStructureNode> iterator() {
        return stream().iterator();
    }

    @Override
    public List<KeyValue> getKeyValuePairs() {
        var kvs = new ArrayList<KeyValue>();
        for (int i = 0; i < size(); i++) {
            kvs.add(new KeyValue(keyNameAt(i), at(i)));
        }
        return kvs;
    }

    @Override
    public List<String> getKeyNames() {
        var names = new ArrayList<String>();
        for (int i = 0; i < size(); i++) {
            names.add(keyNameAt(i));
        }
        return names;
    }

    @Override
    protected String getName() {
        return "context array node";
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public TupleNode mutable() {
        return this;
    }
}
