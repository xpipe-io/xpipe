package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.node.DataStructureNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * A value type represents any node that holds some atomic value, i.e. it has no subtypes.
 */
@JsonTypeName("value")
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class ValueType extends DataType {

    /**
     * Creates a new instance.
     */
    public static ValueType of() {
        return new ValueType();
    }

    @Override
    public String getName() {
        return "value";
    }

    @Override
    public boolean matches(DataStructureNode node) {
        return node.isValue();
    }

    @Override
    public boolean isValue() {
        return true;
    }

    @Override
    public void visit(DataTypeVisitor visitor) {
        visitor.onValue(this);
    }
}
