package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class BrowserStatusBarComp extends SimpleComp {

    OpenFileSystemModel model;

    @Override
    protected Region createSimple() {
        var cc = PlatformThread.sync(BrowserClipboard.currentCopyClipboard);
        var ccCount = Bindings.createStringBinding(
                () -> {
                    if (cc.getValue() != null && cc.getValue().getEntries().size() > 0) {
                        return cc.getValue().getEntries().size() + " file"
                                + (cc.getValue().getEntries().size() > 1 ? "s" : "") + " in clipboard";
                    } else {
                        return null;
                    }
                },
                cc);

        var selectedCount = PlatformThread.sync(Bindings.createIntegerBinding(
                () -> {
                    return model.getFileList().getSelection().size();
                },
                model.getFileList().getSelection()));

        var allCount = PlatformThread.sync(Bindings.createIntegerBinding(
                () -> {
                    return (int) model.getFileList().getAll().getValue().stream()
                            .filter(entry -> !entry.isSynthetic())
                            .count();
                },
                model.getFileList().getAll()));

        var selectedComp = new LabelComp(Bindings.createStringBinding(
                () -> {
                    if (selectedCount.getValue().intValue() == 0) {
                        return null;
                    } else {
                        return selectedCount.getValue() + " / " + allCount.getValue() + " selected";
                    }
                },
                selectedCount,
                allCount));

        var bar = new ToolBar();
        bar.getItems().setAll(new LabelComp(ccCount).createRegion(), new Spacer(), selectedComp.createRegion());
        bar.getStyleClass().add("status-bar");
        bar.setOnDragDetected(event -> {
            event.consume();
            bar.startFullDrag();
        });
        AppFont.small(bar);

        simulateEmptyCell(bar);

        return bar;
    }

    private void simulateEmptyCell(Region r) {
        var emptyEntry = new BrowserFileListCompEntry(r, null, model.getFileList());
        r.setOnMouseClicked(e -> {
            emptyEntry.onMouseClick(e);
        });
        r.setOnMouseDragEntered(event -> {
            emptyEntry.onMouseDragEntered(event);
        });
        r.setOnDragOver(event -> {
            emptyEntry.onDragOver(event);
        });
        r.setOnDragEntered(event -> {
            emptyEntry.onDragEntered(event);
        });
        r.setOnDragDetected(event -> {
            emptyEntry.startDrag(event);
        });
        r.setOnDragExited(event -> {
            emptyEntry.onDragExited(event);
        });
        r.setOnDragDropped(event -> {
            emptyEntry.onDragDrop(event);
        });

        // Use status bar as an extension of file list
        new ContextMenuAugment<>(() -> new BrowserContextMenu(model, null)).augment(new SimpleCompStructure<>(r));
    }
}
