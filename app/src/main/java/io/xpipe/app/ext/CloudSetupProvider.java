package io.xpipe.app.ext;

import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.ModuleLayerLoader;

import java.util.*;

public interface CloudSetupProvider {

    List<CloudSetupProvider> ALL = new ArrayList<>();

    static Optional<CloudSetupProvider> byId(String id) {
        return ALL.stream().filter(d -> d.getId().equalsIgnoreCase(id)).findAny();
    }

    String getId();

    String getNameKey();

    LabelGraphic getGraphic();

    ScanProvider getScan();

    class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL.addAll(ServiceLoader.load(layer, CloudSetupProvider.class).stream()
                    .sorted(Comparator.comparing(p -> p.type().getModule().getName()))
                    .map(p -> p.get())
                    .toList());
        }
    }
}
