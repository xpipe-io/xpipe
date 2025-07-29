package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.util.*;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SyncCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "vaultSync";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdrmz-vpn_lock");
    }

    public Comp<?> create() {
        var prefs = AppPrefs.get();
        AtomicReference<Region> button = new AtomicReference<>();

        var canRestart = new SimpleBooleanProperty(false);
        var testButton = new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
            ThreadHelper.runAsync(() -> {
                var r = DataStorageSyncHandler.getInstance().validateConnection();
                if (r) {
                    Platform.runLater(() -> {
                        button.get().getStyleClass().add(Styles.SUCCESS);
                        canRestart.set(true);
                    });
                }
            });
        });
        testButton.apply(struc -> button.set(struc.get()));
        testButton.padding(new Insets(6, 10, 6, 6));

        var testRow = new HorizontalComp(List.of(testButton))
                .spacing(10)
                .padding(new Insets(10, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));

        var remoteRepo = new TextFieldComp(prefs.storageGitRemote).hgrow();
        remoteRepo.disable(prefs.enableGitStorage.not());

        var builder = new OptionsBuilder();
        builder.addTitle("gitSync")
                .sub(new OptionsBuilder()
                        .pref(prefs.enableGitStorage)
                        .addToggle(prefs.enableGitStorage)
                        .pref(prefs.storageGitRemote)
                        .addComp(remoteRepo.maxWidth(getCompWidth()), prefs.storageGitRemote)
                        .addComp(testRow)
                        .disable(prefs.storageGitRemote.isNull().or(prefs.enableGitStorage.not()))
                        .sub(prefs.getCustomOptions("gitUsername"))
                        .sub(prefs.getCustomOptions("gitPassword"))
                        .sub(prefs.getCustomOptions("gitVaultIdentityStrategy"))
                        .nameAndDescription("browseVault")
                        .addComp(new ButtonComp(AppI18n.observable("browseVaultButton"), () -> {
                            DesktopHelper.browsePathLocal(DataStorage.get().getStorageDir());
                        })));
        return builder.buildComp();
    }
}
