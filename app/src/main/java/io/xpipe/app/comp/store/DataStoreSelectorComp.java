package io.xpipe.app.comp.store;

import com.jfoenix.controls.JFXButton;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DataStoreSelectorComp extends Comp<CompStructure<Button>> {

    DataStoreProvider.DataCategory category;
    Property<DataStore> chosenStore;

    @Override
    public CompStructure<Button> createBase() {
        var button = new JFXButton();
        button.setGraphic(getGraphic());
        button.setOnAction(e -> {
            GuiDsStoreCreator.show(
                    "inProgress",
                    null,
                    null,
                    v -> v.getCategory().equals(category),
                    entry -> {
                        chosenStore.setValue(entry.getStore());
                    },
                    false);
            e.consume();
        });

        Runnable update = () -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var newGraphic = getGraphic();
                button.setGraphic(newGraphic);
                button.layout();
            });
        };

        chosenStore.addListener((c, o, n) -> {
            update.run();
        });

        return new SimpleCompStructure<>(button);
    }

    private Region getGraphic() {
        var provider = chosenStore.getValue() != null
                ? DataStoreProviders.byStoreClass(chosenStore.getValue().getClass())
                        .orElse(null)
                : null;
        var graphic = provider != null ? provider.getDisplayIconFileName(chosenStore.getValue()) : "file_icon.png";
        if (chosenStore.getValue() == null || !(chosenStore.getValue() instanceof FileStore f)) {
            return JfxHelper.createNamedEntry(
                    AppI18n.get("selectStreamStore"), AppI18n.get("openStreamStoreWizard"), graphic);
        } else {
            return JfxHelper.createNamedEntry(f.getFileName(), f.getPath(), graphic);
        }
    }
}
