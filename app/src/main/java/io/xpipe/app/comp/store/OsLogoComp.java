package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.comp.base.StackComp;
import io.xpipe.app.resources.AppResources;
import io.xpipe.core.process.SystemState;
import io.xpipe.core.store.FileNames;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsLogoComp extends SimpleComp {

    private static final Map<String, String> ICONS = new HashMap<>();
    private static final String LINUX_DEFAULT_24 = "linux-24.png";
    private final StoreEntryWrapper wrapper;
    private final ObservableValue<SystemStateComp.State> state;

    public OsLogoComp(StoreEntryWrapper wrapper) {
        this(wrapper, new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    public OsLogoComp(StoreEntryWrapper wrapper, ObservableValue<SystemStateComp.State> state) {
        this.wrapper = wrapper;
        this.state = state;
    }

    @Override
    protected Region createSimple() {
        var img = Bindings.createObjectBinding(
                () -> {
                    if (state.getValue() != SystemStateComp.State.SUCCESS) {
                        return null;
                    }

                    var ps = wrapper.getPersistentState().getValue();
                    if (!(ps instanceof SystemState ons)) {
                        return null;
                    }

                    return getImage(ons.getOsName());
                },
                wrapper.getPersistentState(),
                state);
        var hide = Bindings.createBooleanBinding(
                () -> {
                    return img.get() != null;
                },
                img);
        return new StackComp(List.of(
                        new SystemStateComp(state).hide(hide),
                        PrettyImageHelper.ofFixedSize(img, 24, 24).visible(hide)))
                .createRegion();
    }

    private String getImage(String name) {
        if (name == null) {
            return null;
        }

        if (ICONS.isEmpty()) {
            AppResources.with(AppResources.XPIPE_MODULE, "img/os", file -> {
                try (var list = Files.list(file)) {
                    list.filter(path -> path.toString().endsWith(".png")
                                    && !path.toString().endsWith(LINUX_DEFAULT_24)
                                    && !path.toString().endsWith("-40.png"))
                            .map(path -> FileNames.getFileName(path.toString()))
                            .forEach(path -> {
                                var base = path.replace("-dark", "").replace("-24.png", ".svg");
                                ICONS.put(FileNames.getBaseName(base).split("-")[0], "os/" + base);
                            });
                }
            });
        }

        return ICONS.entrySet().stream()
                .filter(e -> name.toLowerCase().contains(e.getKey())
                        || name.toLowerCase().replaceAll("\\s+", "").contains(e.getKey()))
                .findAny()
                .map(e -> e.getValue())
                .orElse("os/linux.svg");
    }
}
