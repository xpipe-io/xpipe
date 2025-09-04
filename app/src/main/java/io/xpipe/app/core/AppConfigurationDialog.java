package io.xpipe.app.core;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.ScrollComp;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.prefs.AppearanceCategory;
import io.xpipe.app.prefs.EditorCategory;
import io.xpipe.app.prefs.TerminalCategory;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.platform.OptionsBuilder;

import javafx.scene.layout.Region;

public class AppConfigurationDialog {

    public static void showIfNeeded() {
        if (!AppProperties.get().isInitialLaunch()) {
            return;
        }

        var options = new OptionsBuilder()
                .sub(AppearanceCategory.languageChoice())
                .sub(AppearanceCategory.themeChoice())
                .sub(TerminalCategory.terminalChoice(false))
                .sub(EditorCategory.editorChoice())
                .buildComp();
        options.styleClass("initial-setup");
        options.styleClass("prefs-container");

        var scroll = new ScrollComp(options);
        scroll.apply(struc -> {
            struc.get().prefHeightProperty().bind(((Region) struc.get().getContent()).heightProperty());
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
    }
}
