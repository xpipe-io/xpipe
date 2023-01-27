package io.xpipe.app.comp.source.store;

import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.impl.FileSystemStoreChoiceComp;
import io.xpipe.extension.util.DynamicOptionsBuilder;
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
                .addComp(I18n.observable("machine"), new FileSystemStoreChoiceComp(machine), machine)
                .addString(I18n.observable("file"), fileName, true)
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
