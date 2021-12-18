package io.xpipe.core.data.type.callback;

import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;

import java.util.function.Consumer;

public interface DataTypeCallback {

    public static DataTypeCallback flatten(Consumer<DataType> typeConsumer) {
        return new DataTypeCallback() {
            @Override
            public void onValue() {
                typeConsumer.accept(ValueType.of());
            }

            @Override
            public void onTupleBegin(TupleType tuple) {
                typeConsumer.accept(tuple);
            }

            @Override
            public void onArray(ArrayType type) {
                typeConsumer.accept(type);
            }
        };
    }

    default void onValue() {
    }

    default void onTupleBegin(TupleType tuple) {
    }

    default void onTupleEnd() {
    }

    default void onArray(ArrayType type) {
    }
}
