package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.OptionsBuilder;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;

import lombok.SneakyThrows;

public class VaultCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "vault";
    }

    @SneakyThrows
    public Comp<?> create() {
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
                : uh.getUserCount() == 1
                        ? (uh.getActiveUser() != null && uh.getActiveUser().equals("legacy") ? "Legacy" : "Personal")
                        : "Team";

        builder.addTitle("vault")
                .sub(new OptionsBuilder()
                        .name("vaultTypeName" + vaultTypeKey)
                        .description("vaultTypeContent" + vaultTypeKey)
                        .addComp(Comp.empty())
                        .name("userManagement")
                        .description(
                                uh.getActiveUser() != null
                                        ? "userManagementDescription"
                                        : "userManagementDescriptionEmpty")
                        .addComp(uh.createOverview().maxWidth(getCompWidth()))
                        .nameAndDescription("teamVaults")
                        .addComp(Comp.empty())
                        .licenseRequirement("team")
                        .disable(!LicenseProvider.get().getFeature("team").isSupported())
                        .hide(new SimpleBooleanProperty(uh.getUserCount() > 1))
                        .nameAndDescription("syncTeamVaults")
                        .addComp(new ButtonComp(AppI18n.observable("enableGitSync"), () -> AppPrefs.get()
                                .selectCategory("vaultSync")))
                        .licenseRequirement("team")
                        .disable(!LicenseProvider.get().getFeature("team").isSupported())
                        .hide(new SimpleBooleanProperty(
                                DataStorageSyncHandler.getInstance().supportsSync())));
        builder.sub(new OptionsBuilder()
                        .pref(prefs.lockVaultOnHibernation)
                        .addToggle(prefs.lockVaultOnHibernation)
                        .pref(prefs.encryptAllVaultData)
                        .addToggle(encryptVault));
        return builder.buildComp();
    }
}
