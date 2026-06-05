package io.xpipe.app.ext;

import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.util.ModuleLayerLoader;
import io.xpipe.app.webtop.WebtopApp;

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

    WebtopApp getWebtopApp();

    default void handleUnsupported() {}

    class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL.addAll(ServiceLoader.load(layer, CloudSetupProvider.class).stream()
                    .sorted(Comparator.comparing(p -> p.type().getModule().getName()))
                    .map(p -> p.get())
                    .toList());
        }

        @Override
        public boolean initForCli() {
            return false;
        }
    }
}
