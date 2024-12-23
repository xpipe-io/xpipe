package io.xpipe.app.issue;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.PlatformThread;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.concurrent.atomic.AtomicReference;

public class ErrorHandlerDialog {

    public static void show(ErrorEvent event) {
        if (PlatformState.getCurrent() == PlatformState.EXITED || event.isOmitted()) {
            ErrorAction.ignore().handle(event);
            return;
        }

        try {
            PlatformInit.init(true);
            PlatformThread.runLaterIfNeededBlocking(() -> {
                AppMainWindow.initEmpty(true);
            });
        } catch (Throwable t) {
            var platformEvent = ErrorEvent.fromThrowable(t).build();
            ErrorAction.ignore().handle(platformEvent);
            ErrorAction.ignore().handle(event);
            return;
        }

        var modal = new AtomicReference<ModalOverlay>();
        var comp = new ErrorHandlerComp(event, () -> {
            AppDialog.closeDialog(modal.get());
        });
        comp.prefWidth(550);
        var headerId = event.isTerminal() ? "terminalErrorOccured" : "errorOccured";
        modal.set(ModalOverlay.of(headerId, comp, new LabelGraphic.NodeGraphic(() -> {
            var graphic = new FontIcon("mdomz-warning");
            graphic.setIconColor(Color.RED);
            return graphic;
        })));
        modal.get().setOnClose(() -> {
            if (comp.getTakenAction().getValue() == null) {
                ErrorAction.ignore().handle(event);
                comp.getTakenAction().setValue(ErrorAction.ignore());
            }
        });
        AppDialog.showAndWait(modal.get());
    }
}
