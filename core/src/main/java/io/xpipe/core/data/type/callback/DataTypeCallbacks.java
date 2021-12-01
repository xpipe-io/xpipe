package io.xpipe.core.data.type.callback;

import io.xpipe.core.data.generic.DataStructureNodePointer;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.TupleType;

import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DataTypeCallbacks {

    public static DataTypeCallback visitTuples(Consumer<String> newTuple, Runnable endTuple, BiConsumer<String, DataStructureNodePointer> newValue) {
        return new DataTypeCallback() {

            private final Stack<String> keyNames = new Stack<>();
            private final Stack<DataStructureNodePointer.Builder> builders = new Stack<>();

            {
                builders.push(DataStructureNodePointer.builder());
            }

            private boolean isOnTopLevel() {
                return keyNames.size() == 0;
            }

            @Override
            public void onTupleBegin(TupleType tuple) {
                if (!isOnTopLevel()) {
                    newTuple.accept(keyNames.peek());
                }
                tuple.getNames().forEach(n -> {
                    keyNames.push(n);
                    builders.push(builders.peek().copy().name(n));
                    tuple.getTypes().forEach(dt -> dt.traverseType(this));
                });
            }

            @Override
            public void onValue() {
                newValue.accept(keyNames.peek(), builders.peek().build());
                keyNames.pop();
                builders.pop();
            }

            @Override
            public void onTupleEnd() {
                endTuple.run();
            }

            @Override
            public void onArray(ArrayType type) {
                if (!type.isSimple()) {
                    throw new IllegalStateException();
                }

                newValue.accept(keyNames.peek(), builders.peek().build());
            }
        };
    }
}
