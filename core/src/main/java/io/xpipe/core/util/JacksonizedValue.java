package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
public class JacksonizedValue {

    public JacksonizedValue() {
    }

    @SneakyThrows
    public String toString() {
        var tree = JacksonMapper.getDefault().valueToTree(this);
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

        var tree = JacksonMapper.getDefault().valueToTree(this);
        var otherTree = JacksonMapper.getDefault().valueToTree(o);
        return tree.equals(otherTree);
    }

    @Override
    public final int hashCode() {
        var tree = JacksonMapper.getDefault().valueToTree(this);
        return tree.hashCode();
    }
}
