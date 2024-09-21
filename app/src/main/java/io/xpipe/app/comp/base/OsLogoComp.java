package io.xpipe.app.comp.base;

import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.resources.AppResources;
import io.xpipe.core.process.OsNameState;
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
                    if (!(ps instanceof OsNameState ons)) {
                        return null;
                    }

                    return getImage(ons.getOsName());
                },
                wrapper.getPersistentState(),
                state);
        var hide = BindingsHelper.map(img, s -> s != null);
        return new StackComp(
                        List.of(new SystemStateComp(state).hide(hide), new PrettyImageComp(img, 24, 24).visible(hide)))
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
                                    && !path.toString().endsWith(LINUX_DEFAULT_24) && !path.toString().endsWith("-40.png"))
                            .map(path -> FileNames.getFileName(path.toString()))
                            .forEach(path -> {
                                var base = path.replace("-dark", "");
                                ICONS.put(FileNames.getBaseName(base).split("-")[0], "os/" + base);
                            });
                }
            });
        }

        return ICONS.entrySet().stream()
                .filter(e -> name.toLowerCase().contains(e.getKey()))
                .findAny()
                .map(e -> e.getValue())
                .orElse("os/linux");
    }
}
