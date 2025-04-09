package io.xpipe.app.issue;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.PlatformState;

import javafx.application.Platform;
import javafx.scene.paint.Color;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.concurrent.atomic.AtomicReference;

public class ErrorHandlerDialog {

    public static void showAndWait(ErrorEvent event) {
        if (PlatformState.getCurrent() == PlatformState.EXITED || event.isOmitted()) {
            ErrorAction.ignore().handle(event);
            return;
        }

        // There might be unfortunate freezes when there are errors on the platform
        // thread on startup
        if (Platform.isFxApplicationThread() && OperationMode.isInStartup()) {
            ErrorAction.ignore().handle(event);
        }

        try {
            PlatformInit.init(true);
            AppMainWindow.init(true);
        } catch (Throwable t) {
            var platformEvent = ErrorEvent.fromThrowable(t).build();
            ErrorAction.ignore().handle(platformEvent);
            ErrorAction.ignore().handle(event);
            return;
        }

        try {
            var modal = new AtomicReference<ModalOverlay>();
            var comp = new ErrorHandlerComp(event, () -> {
                AppDialog.closeDialog(modal.get());
            });
            comp.prefWidth(event.getThrowable() != null ? 600 : 500);
            var headerId = event.isTerminal() ? "terminalErrorOccured" : "errorOccured";
            var errorModal = ModalOverlay.of(headerId, comp, new LabelGraphic.NodeGraphic(() -> {
                var graphic = new FontIcon("mdomz-warning");
                graphic.setIconColor(Color.RED);
                return graphic;
            }));
            if (event.getThrowable() != null && event.isReportable()) {
                errorModal.addButton(new ModalButton(
                        "stackTrace",
                        () -> {
                            var content =
                                    new ErrorDetailsComp(event).prefWidth(650).prefHeight(750);
                            var detailsModal = ModalOverlay.of("errorDetails", content);
                            detailsModal.show();
                        },
                        false,
                        false));
            }
            if (event.isReportable()) {
                errorModal.addButton(new ModalButton(
                        "report",
                        () -> {
                            if (UserReportComp.show(event)) {
                                comp.getTakenAction().setValue(ErrorAction.ignore());
                                errorModal.close();
                            }
                        },
                        false,
                        false));
                errorModal.addButtonBarComp(Comp.hspacer());
            }
            var hasCustomActions = event.getCustomActions().size() > 0 || event.getLink() != null;
            var hideOk = hasCustomActions;
            if (!hideOk) {
                errorModal.addButton(ModalButton.ok());
            }
            modal.set(errorModal);
            AppDialog.showAndWait(modal.get());
            if (comp.getTakenAction().getValue() == null) {
                ErrorAction.ignore().handle(event);
                comp.getTakenAction().setValue(ErrorAction.ignore());
            }
        } catch (Throwable t) {
            ErrorAction.ignore().handle(ErrorEvent.fromThrowable(t).build());
            ErrorAction.ignore().handle(event);
        }
    }
}
