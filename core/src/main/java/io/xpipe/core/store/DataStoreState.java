package io.xpipe.core.store;

import io.xpipe.core.util.JacksonMapper;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class DataStoreState {

    public DataStoreState() {}

    protected static <T> T useNewer(T older, T newer) {
        return newer != null ? newer : older;
    }

    public abstract void merge(DataStoreState newer);

    @SneakyThrows
    public String toString() {
        var tree = JacksonMapper.getDefault().valueToTree(this);
        return tree.toPrettyString();
    }

    @SneakyThrows
    public DataStoreState deepCopy() {
        return JacksonMapper.getDefault().treeToValue(JacksonMapper.getDefault().valueToTree(this), getClass());
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
