package io.xpipe.core.data.type;

import io.xpipe.core.data.node.DataStructureNodePointer;

import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DataTypeVisitors {

    /**
     * Creates a visitor that sequentially visits all subtypes.
     */
    public static DataTypeVisitor flatten(Consumer<DataType> typeConsumer) {
        return new DataTypeVisitor() {
            @Override
            public void onValue(ValueType type) {
                typeConsumer.accept(type);
            }

            @Override
            public void onTuple(TupleType type) {
                typeConsumer.accept(type);
                type.getTypes().forEach(t -> t.visit(this));
            }

            @Override
            public void onArray(ArrayType type) {
                typeConsumer.accept(type);
            }

            @Override
            public void onWildcard(WildcardType type) {
                typeConsumer.accept(type);
            }
        };
    }

    /**
     * Creates a visitor that allows for visiting possible recursive columns of table.
     */
    public static DataTypeVisitor table(
            Consumer<String> newTuple,
            Runnable endTuple,
            BiConsumer<String, DataStructureNodePointer> newValue) {
        return new DataTypeVisitor() {
            private final Stack<TupleType> tuples = new Stack<>();
            private final Stack<Integer> keyIndices = new Stack<>();

            private boolean isOnTopLevel() {
                return tuples.size() <= 1;
            }

            private void onAnyValue() {
                var pointer = DataStructureNodePointer.builder();
                for (int index : keyIndices) {
                    pointer.index(index);
                }
                var p = pointer.build();
                newValue.accept(tuples.peek().getNames().get(keyIndices.peek()), p);

                moveIndex();
            }

            private void moveIndex() {
                var index = keyIndices.pop();
                index++;
                keyIndices.push(index);
            }

            @Override
            public void onValue(ValueType type) {
                onAnyValue();
            }

            @Override
            public void onWildcard(WildcardType type) {
                onAnyValue();
            }

            @Override
            public void onTuple(TupleType tuple) {
                if (!isOnTopLevel()) {
                    moveIndex();
                }

                tuples.push(tuple);
                keyIndices.push(0);

                if (!isOnTopLevel()) {
                    newTuple.accept(tuples.peek().getNames().get(keyIndices.peek()));
                }

                tuple.getTypes().forEach(t -> t.visit(this));
                endTuple.run();
                tuples.pop();
                keyIndices.pop();
            }

            @Override
            public void onArray(ArrayType type) {
                onAnyValue();
            }
        };
    }
}
