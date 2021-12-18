package io.xpipe.core.data.type;

public interface DataTypeVisitor {

    default void onValue(ValueType type) {
    }

    default void onTuple(TupleType type) {
    }

    default void onArray(ArrayType type) {
    }

    default void onWildcard(WildcardType type) {
    }
}
