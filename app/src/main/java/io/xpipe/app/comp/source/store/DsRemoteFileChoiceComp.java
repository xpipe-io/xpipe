package io.xpipe.app.comp.source.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileSystemStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class DsRemoteFileChoiceComp extends SimpleComp {

    Property<DataStore> store;

    @Override
    protected Region createSimple() {
        var machine = new SimpleObjectProperty<FileSystemStore>();
        var fileName = new SimpleStringProperty();
        return new DynamicOptionsBuilder(false)
                .addString(AppI18n.observable("file"), fileName, true)
                .bind(
                        () -> {
                            if (fileName.get() == null || machine.get() == null) {
                                return null;
                            }

                            return FileStore.builder()
                                    .fileSystem(machine.get())
                                    .file(fileName.get())
                                    .build();
                        },
                        store)
                .build();
    }
}
