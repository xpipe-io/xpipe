package io.xpipe.app.prefs;


import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.*;

import io.xpipe.core.FilePath;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.BaseRegionBuilder;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
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

    public BaseRegionBuilder<?,?> create() {
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
        testButton.apply(struc -> button.set(struc));
        testButton.padding(new Insets(6, 10, 6, 6));

        var testRow = new HorizontalComp(List.of(testButton))
                .spacing(10)
                .padding(new Insets(10, 0, 0, 0))
                .apply(struc -> struc.setAlignment(Pos.CENTER_LEFT));

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
                        .sub(prefs.getCustomOptions("syncToPlainDirectory"))
                        .sub(prefs.getCustomOptions("gitUsername"))
                        .sub(prefs.getCustomOptions("gitPassword"))
                        .sub(prefs.getCustomOptions("gitVaultIdentityStrategy"))
                        .pref(prefs.syncMode)
                        .addComp(ChoiceComp.ofTranslatable(prefs.syncMode, Arrays.asList(SyncMode.values()), false), prefs.syncMode)
                        .addComp(createManualControls())
                        .hide(prefs.syncMode.isNotEqualTo(SyncMode.MANUAL).or(prefs.enableGitStorage.not()))
                        .nameAndDescription("browseVault")
                        .addComp(new ButtonComp(AppI18n.observable("browseVaultButton"), () -> {
                            DesktopHelper.browseFile(DataStorage.get().getStorageDir());
                        })));
        return builder.buildComp();
    }

    private RegionBuilder<?> createManualControls() {
        var busy = new SimpleBooleanProperty();
        var busyIcon = new LoadingIconComp(busy, AppFontSizes::base);

        var pullButton = new ButtonComp(AppI18n.observable("pullChanges"), new LabelGraphic.IconGraphic("mdi2d-download"), () -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    DataStorage.get().pullManually();
                });
            });
        });

        var pushButton = new ButtonComp(AppI18n.observable("pushChanges"), new LabelGraphic.IconGraphic("mdi2u-upload"), () -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    DataStorage.get().pushManually();
                });
            });
        });

        var terminalButton = new ButtonComp(AppI18n.observable("openTerminal"), new LabelGraphic.IconGraphic("mdi2c-console"), () -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    TerminalLaunch.builder().command(LocalShell.getShell()).directory(FilePath.of(DataStorage.get().getStorageDir())).launch();
                });
            });
        });

        var box = new HorizontalComp(List.of(pullButton, pushButton, terminalButton, busyIcon)).spacing(10).apply(struc -> struc.setAlignment(Pos.CENTER_LEFT));
        return box;
    }
}
