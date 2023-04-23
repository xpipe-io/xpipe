package io.xpipe.core.store;

import java.util.Map;

public interface FixedHierarchyStore extends DataStore {

    Map<String, DataStore> listChildren() throws Exception;
}
