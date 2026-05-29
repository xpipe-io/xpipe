package io.xpipe.app.core;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.ScrollComp;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.PlatformState;
import io.xpipe.app.prefs.*;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.OsType;

import javafx.application.Platform;
import javafx.scene.layout.Region;

public class AppConfigurationDialog {

    public static void showIfNeeded() {
        if (!AppProperties.get().isInitialLaunch() || AppProperties.get().isTest()) {
            return;
        }

        // We might be running in background mode
        if (PlatformState.getCurrent() != PlatformState.RUNNING) {
            return;
        }

        // Enforce that everything is created on the platform thread to align with prefs comp
        Platform.runLater(() -> {
            var options = new OptionsBuilder()
                    .sub(PersonalizationCategory.languageChoice())
                    .sub(PersonalizationCategory.themeChoice())
                    .sub(TerminalCategory.terminalChoice(false));

            if (OsType.ofLocal() != OsType.WINDOWS
                    && AppPrefs.get().terminalMultiplexer().getValue() != null) {
                options.sub(TerminalCategory.terminalMultiplexerChoice());
            }

            var optionsComp = options.sub(EditorCategory.editorChoice())
                    .sub(PasswordManagerCategory.passwordManagerChoice())
                    .buildComp();
            optionsComp.style("initial-setup");
            optionsComp.style("prefs-container");

            var scroll = new ScrollComp(optionsComp);
            scroll.apply(struc -> {
                struc.prefHeightProperty().bind(((Region) struc.getContent()).heightProperty());
            });
            scroll.minWidth(650);
            scroll.prefWidth(650);

            var modal = ModalOverlay.of("initialSetup", scroll);
            modal.addButton(new ModalButton(
                    "docs",
                    () -> {
                        DocumentationLink.INTRO.open();
                    },
                    false,
                    false));
            modal.addButton(ModalButton.ok());
            AppDialog.show(modal);
        });
    }
}
