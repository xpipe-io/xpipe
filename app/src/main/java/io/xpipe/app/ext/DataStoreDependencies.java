package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStoreEntryRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DataStoreDependencies {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<DataStoreEntryRef<?>> of(Object... dependencies) {
        var l = new ArrayList<DataStoreEntryRef<?>>();
        for (Object dependency : dependencies) {
            if (dependency instanceof DataStoreEntryRef<?> r) {
                l.add(r);
            } else if (dependency instanceof List li) {
                l.addAll(li.stream().filter(o -> o != null).toList());
            }
        }
        return l;
    }

    public static List<DataStoreEntryRef<?>> of(DataStoreEntryRef<?>... dependencies) {
        return Arrays.stream(dependencies).filter(Objects::nonNull).toList();
    }

    public static <T extends DataStore> List<DataStoreEntryRef<?>> of(List<DataStoreEntryRef<T>> refs) {
        return refs.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
