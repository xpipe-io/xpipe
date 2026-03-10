package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStoreEntryRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataStoreDependencies {

    public static List<DataStoreEntryRef<?>> of(Object... dependencies) {
        var l = new ArrayList<DataStoreEntryRef<?>>();
        for (Object dependency : dependencies) {
            if (dependency instanceof DataStoreEntryRef<?> r) {
                l.add(r);
            } else if (dependency instanceof List<?> li) {
                l.addAll(of(li));
            }
        }
        return l;
    }

    public static List<DataStoreEntryRef<?>> of(DataStoreEntryRef<?>... dependencies) {
        return Arrays.stream(dependencies).toList();
    }

    public static <T extends DataStore> List<DataStoreEntryRef<?>> of(List<DataStoreEntryRef<T>> refs) {
        return new ArrayList<>(refs);
    }
}
