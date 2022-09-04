package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class LinkedTupleNode extends TupleNode {

    private final List<TupleNode> tupleNodes;
    private List<KeyValue> joined;

    public LinkedTupleNode(List<TupleNode> tupleNodes) {
        this.tupleNodes = new ArrayList<>(tupleNodes);
    }

    @Override
    public String keyNameAt(int index) {
        int list = getTupleNodeForIndex(index);
        return tupleNodes.get(list).keyNameAt(getLocalIndex(list, index));
    }

    @Override
    public List<KeyValue> getKeyValuePairs() {
        // Lazy initialize joined list
        if (joined == null) {
            this.joined = new ArrayList<>();
            for (var n : tupleNodes) {
                joined.addAll(n.getKeyValuePairs());
            }
            this.joined = Collections.unmodifiableList(joined);
        }

        return joined;
    }

    @Override
    public List<String> getKeyNames() {
        return getKeyValuePairs().stream().map(KeyValue::key).toList();
    }

    @Override
    public List<DataStructureNode> getNodes() {
        return getKeyValuePairs().stream().map(KeyValue::value).toList();
    }

    @Override
    protected String getName() {
        return "linked tuple node";
    }

    @Override
    public boolean isMutable() {
        return tupleNodes.stream().allMatch(DataStructureNode::isMutable);
    }

    @Override
    public DataStructureNode clear() {
        tupleNodes.forEach(DataStructureNode::clear);
        return this;
    }

    @Override
    public DataStructureNode set(int index, DataStructureNode node) {
        return super.set(index, node);
    }

    @Override
    public DataStructureNode put(String keyName, DataStructureNode node) {
        return super.put(keyName, node);
    }

    @Override
    public DataStructureNode remove(int index) {
        return super.remove(index);
    }

    @Override
    public DataStructureNode remove(String keyName) {
        return super.remove(keyName);
    }

    @Override
    public DataType determineDataType() {
        return TupleType.of(getKeyNames(), getNodes().stream().map(DataStructureNode::determineDataType).toList());
    }

    @Override
    public DataStructureNode at(int index) {
        int list = getTupleNodeForIndex(index);
        return tupleNodes.get(list).at(getLocalIndex(list, index));
    }

    @Override
    public DataStructureNode forKey(String name) {
        for (var ar : tupleNodes) {
            var r = ar.forKeyIfPresent(name);
            if (r.isPresent()) {
                return r.get();
            }
        }
        throw new IllegalArgumentException("Invalid key " + name);
    }

    @Override
    public Optional<DataStructureNode> forKeyIfPresent(String name) {
        for (var ar : tupleNodes) {
            var r = ar.forKeyIfPresent(name);
            if (r.isPresent()) {
                return r;
            }
        }
        return Optional.empty();
    }

    @Override
    public Stream<DataStructureNode> stream() {
        return getNodes().stream();
    }

    @Override
    public void forEach(Consumer<? super DataStructureNode> action) {
        for (var ar : tupleNodes) {
            ar.forEach(action);
        }
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
    public String toString() {
        return "LinkedTupleNode(" + size() + ")";
    }


    @Override
    public int size() {
        return tupleNodes.stream().mapToInt(TupleNode::size).sum();
    }

    private int getLocalIndex(int listIndex, int absIndex) {
        int current = 0;
        for (int i = 0; i < listIndex; i++) {
            current += tupleNodes.get(i).size();
        }
        return absIndex - current;
    }

    private int getTupleNodeForIndex(int index) {
        int current = 0;
        for (var a : tupleNodes) {
            if (index < current + a.size()) {
                return tupleNodes.indexOf(a);
            } else {
                current += a.size();
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public TupleNode mutable() {
        if (isMutable()) {
            return this;
        }

        return new LinkedTupleNode(tupleNodes.stream().map(n -> n.isMutable() ? n : n.mutable()).toList());
    }
}
