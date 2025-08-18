package io.xpipe.app.browser.file;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.core.FilePath;

import javafx.beans.property.SimpleObjectProperty;

public class BrowserAlerts {

    public static FileConflictChoice showFileConflictAlert(FilePath file, boolean multiple) {
        var choice = new SimpleObjectProperty<FileConflictChoice>();
        var key = multiple ? "fileConflictAlertContentMultiple" : "fileConflictAlertContent";
        var w = multiple ? 700 : 400;
        var modal = ModalOverlay.of(
                "fileConflictAlertTitle",
                AppDialog.dialogText(AppI18n.observable(key, file)).prefWidth(w));
        modal.addButton(new ModalButton("cancel", () -> choice.set(FileConflictChoice.CANCEL), true, false));
        if (multiple) {
            modal.addButton(new ModalButton("skip", () -> choice.set(FileConflictChoice.SKIP), true, false));
            modal.addButton(new ModalButton("skipAll", () -> choice.set(FileConflictChoice.SKIP_ALL), true, false));
        }
        modal.addButton(new ModalButton("replace", () -> choice.set(FileConflictChoice.REPLACE), true, false));
        if (multiple) {
            modal.addButton(
                    new ModalButton("replaceAll", () -> choice.set(FileConflictChoice.REPLACE_ALL), true, false));
        }
        modal.addButton(new ModalButton("rename", () -> choice.set(FileConflictChoice.RENAME), true, false));
        if (multiple) {
            modal.addButton(new ModalButton("renameAll", () -> choice.set(FileConflictChoice.RENAME_ALL), true, false));
        }
        modal.showAndWait();
        return choice.get() != null ? choice.get() : FileConflictChoice.CANCEL;
    }

    public enum FileConflictChoice {
        CANCEL,
        SKIP,
        SKIP_ALL,
        REPLACE,
        REPLACE_ALL,
        RENAME,
        RENAME_ALL
    }
}
