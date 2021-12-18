package io.xpipe.core.data.type;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.callback.DataTypeCallback;

public class WildcardType implements DataType {

    private WildcardType() {

    }

    public static WildcardType of() {
        return new WildcardType();
    }

    @Override
    public String getName() {
        return "wildcard";
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
    public void traverseType(DataTypeCallback cb) {

    }
}
