package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SyncCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "sync";
    }


    private static void showHelpAlert() {
        AppWindowHelper.showAlert(
                alert -> {
                    alert.setTitle(AppI18n.get("gitVault"));
                    alert.setAlertType(Alert.AlertType.NONE);

                    var activated = AppI18n.get().getMarkdownDocumentation("app:vault");
                    var markdown = new MarkdownComp(activated, s -> s)
                            .prefWidth(550)
                            .prefHeight(550)
                            .createRegion();
                    alert.getDialogPane().setContent(markdown);
                    alert.getButtonTypes().add(ButtonType.OK);
                },
                buttonType -> {});
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

        var restartButton = new ButtonComp(AppI18n.observable("restart"), new FontIcon("mdi2r-restart"), () -> {
            OperationMode.restart();
        });
        restartButton.visible(canRestart);
        restartButton.padding(new Insets(6, 10, 6, 6));

        var testRow = new HorizontalComp(
                List.of(testButton, restartButton))
                .spacing(10)
                .padding(new Insets(10, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));

        var remoteRepo = new TextFieldComp(prefs.storageGitRemote).hgrow();
        var helpButton = new ButtonComp(AppI18n.observable("help"), new FontIcon("mdi2h-help-circle-outline"), () -> {
            showHelpAlert();
        });
        var remoteRow = new HorizontalComp(List.of(remoteRepo, helpButton)).spacing(10);
        remoteRow.apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));

        var builder = new OptionsBuilder();
        builder.addTitle("sync")
                .sub(new OptionsBuilder()
                        .name("enableGitStorage")
                        .description("enableGitStorageDescription")
                        .addToggle(prefs.enableGitStorage)
                        .nameAndDescription("storageGitRemote")
                        .addComp(remoteRow, prefs.storageGitRemote)
                        .disable(prefs.enableGitStorage.not())
                        .addComp(testRow)
                        .disable(prefs.storageGitRemote.isNull().or(prefs.enableGitStorage.not()))
                        .addComp(prefs.getCustomComp("gitVaultIdentityStrategy"))
                        .nameAndDescription("openDataDir")
                        .addComp(new ButtonComp(AppI18n.observable("openDataDirButton"), () -> {
                            DesktopHelper.browsePathLocal(DataStorage.get().getDataDir());
                        })));
        return builder.buildComp();
    }
}
