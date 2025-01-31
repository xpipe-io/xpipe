package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
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
            if (!newValue
                    && !AppWindowHelper.showConfirmationAlert(
                            "confirmVaultUnencryptTitle",
                            "confirmVaultUnencryptHeader",
                            "confirmVaultUnencryptContent")) {
                Platform.runLater(() -> {
                    encryptVault.set(true);
                });
                return;
            }

            prefs.encryptAllVaultData.setValue(newValue);
        });

        builder.addTitle("vaultUsers")
                .sub(new OptionsBuilder()
                        .nameAndDescription("personalVault")
                        .addComp(Comp.empty())
                        .hide(new SimpleBooleanProperty(DataStorageUserHandler.getInstance().getUserCount() > 1))
                        .name("userManagement")
                        .description(
                                DataStorageUserHandler.getInstance().getActiveUser() != null
                                        ? "userManagementDescription"
                                        : "userManagementDescriptionEmpty")
                        .addComp(DataStorageUserHandler.getInstance().createOverview())
                        .nameAndDescription("teamVaults")
                        .addComp(Comp.empty())
                        .licenseRequirement("team")
                        .disable(!LicenseProvider.get().getFeature("team").isSupported())
                        .hide(new SimpleBooleanProperty(DataStorageUserHandler.getInstance().getUserCount() > 1))
                        .nameAndDescription("syncTeamVaults")
                        .addComp(new ButtonComp(AppI18n.observable("enableGitSync"), () -> AppPrefs.get()
                                .selectCategory("sync")))
                        .licenseRequirement("team")
                        .disable(!LicenseProvider.get().getFeature("team").isSupported())
                        .hide(new SimpleBooleanProperty(
                                DataStorageSyncHandler.getInstance().supportsSync())));
        builder.addTitle("vaultSecurity")
                .sub(new OptionsBuilder()
                        .pref(prefs.lockVaultOnHibernation)
                        .addToggle(prefs.lockVaultOnHibernation)
                        .pref(prefs.encryptAllVaultData)
                        .addToggle(encryptVault)
                        .disable(DataStorageUserHandler.getInstance().getUserCount() > 1)
                );
        builder.addTitle("vault")
                .sub(new OptionsBuilder()
                        .nameAndDescription("browseVault")
                        .addComp(new ButtonComp(AppI18n.observable("browseVaultButton"), () -> {
                            DesktopHelper.browsePathLocal(DataStorage.get().getStorageDir());
                        })));
        return builder.buildComp();
    }
}
