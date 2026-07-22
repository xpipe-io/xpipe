package io.xpipe.app.prefs;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LicenseProvider;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import lombok.SneakyThrows;

import java.util.Arrays;

public class VaultCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "vault";
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
                }));
                modal.showAndWait();
            } else {
                prefs.encryptAllVaultData.setValue(true);
            }
        });

        var uh = DataStorageUserHandler.getInstance();
        var vaultTypeKey = uh.getUserCount() == 0
                ? "Default"
                : uh.getUserCount() == 1 && uh.getVaultAuthenticationType() != VaultAuthentication.GROUP
                        ? (uh.getActiveUser() != null && uh.getActiveUser().equals("legacy") ? "Legacy" : "Personal")
                        : "Team";

        var authChoice =
                ChoiceComp.ofTranslatable(prefs.vaultAuthentication, Arrays.asList(VaultAuthentication.values()), true);
        authChoice.apply(struc -> struc.setOpacity(1.0));
        authChoice.maxWidth(600);

        var groupStrategy =
                new SimpleObjectProperty<>(uh.getActiveUser() != null ? uh.getGroupStrategy(uh.getActiveUser()) : null);
        groupStrategy.addListener((obs, ov, nv) -> {
            uh.setCurrentGroupStrategy(nv);
        });

        builder.title("vault");
        builder.sub(new OptionsBuilder()
                .pref(prefs.encryptAllVaultData)
                .addToggle(encryptVault)
                .pref(prefs.hideVaultEntryNames)
                .addToggle(prefs.hideVaultEntryNames)
                .hide(encryptVault.not()));
        return builder.buildComp();
    }
}
