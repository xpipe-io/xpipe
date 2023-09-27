package io.xpipe.app.comp;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.issue.ErrorEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.nio.file.Path;

public class DeveloperTabComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var button = new ButtonComp(AppI18n.observable("Throw exception"), null, () -> {
            throw new IllegalStateException();
        });

        var button2 = new ButtonComp(AppI18n.observable("Throw exception with file"), null, () -> {
            try {
                throw new IllegalStateException();
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex)
                        .attachment(Path.of("extensions.txt"))
                        .build()
                        .handle();
            }
        });

        var button3 = new ButtonComp(AppI18n.observable("Exit"), null, () -> {
            System.exit(0);
        });

        var button6 = new ButtonComp(AppI18n.observable("Restart"), null, () -> {
            OperationMode.restart();
        });

        var button4 = new ButtonComp(AppI18n.observable("Throw terminal exception"), null, () -> {
            try {
                throw new IllegalStateException();
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
            }
        });

        var button5 = new ButtonComp(AppI18n.observable("Operation mode null"), null, OperationMode::close);

        return new HBox(
                button.createRegion(),
                button2.createRegion(),
                button3.createRegion(),
                button4.createRegion(),
                button5.createRegion(),
                button6.createRegion());
    }
}
