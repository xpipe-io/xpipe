package io.xpipe.app.comp.source.store;

import com.jfoenix.controls.JFXButton;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.DataStoreProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DataStoreSelectorComp extends Comp<CompStructure<Button>> {

    DataStoreProvider.Category category;
    Property<DataStore> chosenStore;

    @Override
    public CompStructure<Button> createBase() {
        var button = new JFXButton();
        button.setGraphic(getGraphic());
        button.setOnAction(e -> {
            GuiDsStoreCreator.show("inProgress", null, null, category, entry -> {
                chosenStore.setValue(entry.getStore());
            });
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
        var graphic = provider != null ? provider.getDisplayIconFileName() : "file_icon.png";
        if (chosenStore.getValue() == null || !(chosenStore.getValue() instanceof FileStore f)) {
            return JfxHelper.createNamedEntry(
                    I18n.get("selectStreamStore"), I18n.get("openStreamStoreWizard"), graphic);
        } else {
            return JfxHelper.createNamedEntry(
                    f.getFileName().toString(), f.getFile().toString(), graphic);
        }
    }
}
