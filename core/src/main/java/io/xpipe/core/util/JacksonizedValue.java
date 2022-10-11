package io.xpipe.core.util;

import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class JacksonizedValue {

    public JacksonizedValue() {
    }

    @SneakyThrows
    public final String toString() {
        var tree = JacksonMapper.newMapper().valueToTree(this);
        return tree.toPrettyString();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o != null && getClass() != o.getClass()) {
            return false;
        }

        var tree = JacksonMapper.newMapper().valueToTree(this);
        var otherTree = JacksonMapper.newMapper().valueToTree(o);
        return tree.equals(otherTree);
    }

    @Override
    public final int hashCode() {
        var tree = JacksonMapper.newMapper().valueToTree(this);
        return tree.hashCode();
    }

}
