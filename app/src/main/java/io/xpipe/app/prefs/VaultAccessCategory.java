package io.xpipe.app.prefs;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.AuthModuleProvider;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;

import lombok.SneakyThrows;

public class VaultAccessCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "vaultAccess";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2d-database-lock-outline");
    }

    @SneakyThrows
    public BaseRegionBuilder<?, ?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();

        var encryptVault = new SimpleBooleanProperty(prefs.encryptAllVaultData().get());
        encryptVault.addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                var modal = ModalOverlay.of(
                        "confirmVaultUnencryptTitle", AppDialog.dialogTextKey("confirmVaultUnencryptContent"));
                modal.addButton(ModalButton.cancel(() -> {
                    Platform.runLater(() -> {
                        encryptVault.set(true);
                    });
                }));
                modal.addButton(ModalButton.ok(() -> {
                    prefs.encryptAllVaultData.setValue(false);
                    ThreadHelper.runAsync(() -> {
                        DataStorage.get().refreshStoreEntriesEncryption();
                        DataStorage.get().saveAsync();
                    });
                }));
                modal.showAndWait();
            } else {
                prefs.encryptAllVaultData.setValue(true);
                ThreadHelper.runAsync(() -> {
                    DataStorage.get().refreshStoreEntriesEncryption();
                    DataStorage.get().saveAsync();
                });
            }
        });

        builder.title("vaultAccess").sub(AuthModuleProvider.get().createVaultAccessOptions());
        builder.sub(new OptionsBuilder()
                        .pref(prefs.encryptAllVaultData)
                        .addToggle(encryptVault)
                        .pref(prefs.hideVaultEntryNames)
                        .addToggle(prefs.hideVaultEntryNames)
                        .hide(encryptVault.not()))
                .disable(prefs.enableGitStorage.not());
        return builder.buildComp();
    }
}
