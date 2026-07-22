package io.xpipe.app.storage;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.LoadingIconComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.InPlaceSecretValue;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class DataStorageMigrationDialog {

    public static void show() {
        var content = AppDialog.dialogTextKey("storageMigrationContent");
        var busy = new SimpleBooleanProperty();
        var modal = ModalOverlay.of("storageMigrationTitle", content);
        modal.addButtonBarComp(new LabelComp(AppI18n.observable("applyingVaultChanges")).visible(busy));
        modal.addButtonBarComp(RegionBuilder.hspacer());
        modal.addButton(ModalButton.cancel());
        modal.addButton(new ModalButton(
                        "apply",
                        () -> {
                            ThreadHelper.runFailableAsync(() -> {
                                if (busy.get()) {
                                    return;
                                }

                                BooleanScope.executeExclusive(busy, () -> {
                                    DataStorageMigration.migrate();
                                    modal.close();
                                });
                            });
                        },
                        false,
                        true))
                .augment(button -> {
                    button.graphicProperty()
                            .bind(Bindings.createObjectBinding(
                                    () -> {
                                        return busy.get()
                                                ? new LoadingIconComp(busy, AppFontSizes::base)
                                                  .style("busy-loading-icon")
                                                  .build()
                                                : null;
                                    },
                                    PlatformThread.sync(busy)));
                    button.textProperty()
                            .bind(Bindings.createStringBinding(
                                    () -> {
                                        return !busy.get() ? AppI18n.get("apply") : null;
                                    },
                                    PlatformThread.sync(busy),
                                    AppI18n.activeLanguage()));;
                });
        modal.show();
    }
}
