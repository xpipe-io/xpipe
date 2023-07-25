package io.xpipe.app.comp.base;

import io.xpipe.app.comp.storage.store.StoreEntryWrapper;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.impl.FileNames;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.Region;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsLogoComp extends SimpleComp {

    private final StoreEntryWrapper wrapper;

    public OsLogoComp(StoreEntryWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    protected Region createSimple() {
        var img = Bindings.createObjectBinding(
                () -> {
                    return wrapper.getState().getValue() == DataStoreEntry.State.COMPLETE_AND_VALID
                            ? getImage(wrapper.getInformation().get()) : null;
                },
                wrapper.getState(), wrapper.getInformation());
        return new StackComp(List.of(new SystemStateComp(wrapper).hide(img.isNotNull()), new PrettyImageComp(img, 24, 24))).createRegion();
    }

    private static final Map<String, String> ICONS = new HashMap<>();

    private String getImage(String name) {
        if (name == null) {
            return null;
        }

        if (ICONS.size() == 0) {
            AppResources.withResource(AppResources.XPIPE_MODULE, "img/os", ModuleLayer.boot(), file -> {
                try (var list = Files.list(file)) {
                    list.forEach(path -> {
                        var fileName = path.getFileName().toString();
                        ICONS.put(FileNames.getBaseName(fileName).split("-")[0], "os/" + fileName);
                    });
                }
            });
        }

        var found = ICONS.entrySet().stream().filter(e->name.toLowerCase().contains(e.getKey())).findAny().map(e->e.getValue()).orElse(null);
        return found;
    }
}
