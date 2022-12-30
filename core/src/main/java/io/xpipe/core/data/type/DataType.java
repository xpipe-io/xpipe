package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.data.node.DataStructureNode;

import java.util.Optional;

/**
 * Represents the type of a {@link DataStructureNode} object.
 * To check whether a {@link DataStructureNode} instance conforms to the specified type,
 * the method {@link #matches(DataStructureNode)} can be used.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
public abstract class DataType {

    /**
     * Returns the readable name of this data type that can be used for error messages.
     */
    public abstract String getName();

    /**
     * Checks whether a node can be converted to this data type.
     */
    public abstract Optional<DataStructureNode> convert(DataStructureNode node);

    /**
     * Checks whether a node conforms to this data type.
     */
    public abstract boolean matches(DataStructureNode node);

    /**
     * Checks whether this type is a tuple.
     */
    public boolean isTuple() {
        return false;
    }

    /**
     * Checks whether this type is a wildcard.
     */
    public boolean isWildcard() {
        return false;
    }

    /**
     * Checks whether this type is an array.
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Checks whether this type is a value.
     */
    public boolean isValue() {
        return false;
    }

    /**
     * Casts this type to a wildcard type if possible.
     *
     * @throws UnsupportedOperationException if the type is not a wildcard type
     */
    public final WildcardType asWildcard() {
        if (!isWildcard()) {
            throw new UnsupportedOperationException(getName() + " is not a wildcard type");
        }

        return (WildcardType) this;
    }

    /**
     * Casts this type to a value type if possible.
     *
     * @throws UnsupportedOperationException if the type is not a value type
     */
    public final ValueType asValue() {
        if (!isValue()) {
            throw new UnsupportedOperationException(getName() + " is not a value type");
        }

        return (ValueType) this;
    }

    /**
     * Casts this type to a tuple type if possible.
     *
     * @throws UnsupportedOperationException if the type is not a tuple type
     */
    public final TupleType asTuple() {
        if (!isTuple()) {
            throw new UnsupportedOperationException(getName() + " is not a tuple type");
        }

        return (TupleType) this;
    }

    /**
     * Casts this type to an array type if possible.
     *
     * @throws UnsupportedOperationException if the type is not an array type
     */
    public final ArrayType asArray() {
        if (!isArray()) {
            throw new UnsupportedOperationException(getName() + " is not an array type");
        }

        return (ArrayType) this;
    }

    /**
     * Visits this type using a {@link DataTypeVisitor} instance.
     */
    public abstract void visit(DataTypeVisitor visitor);
}
