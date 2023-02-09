package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.node.DataStructureNode;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Optional;

/**
 * A wildcard type matches any {@link DataStructureNode} instance.
 * For simplicity reasons it is not possible to further specify a wildcard instance to only match a certain
 * subset of {@link DataStructureNode} instance that fullfil a certain property.
 */
@JsonTypeName("wildcard")
@EqualsAndHashCode(callSuper = false)
@Value
public class WildcardType extends DataType {

    private WildcardType() {}

    /**
     * Creates a new instance.
     */
    public static WildcardType of() {
        return new WildcardType();
    }

    @Override
    public String getName() {
        return "wildcard";
    }

    @Override
    public Optional<DataStructureNode> convert(DataStructureNode node) {
        return Optional.of(node);
    }

    @Override
    public boolean matches(DataStructureNode node) {
        return true;
    }

    @Override
    public boolean isWildcard() {
        return true;
    }

    @Override
    public void visit(DataTypeVisitor visitor) {
        visitor.onWildcard(this);
    }
}
