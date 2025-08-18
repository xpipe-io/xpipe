package io.xpipe.app.browser.file;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.augment.ContextMenuAugment;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.HumanReadableFormat;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class BrowserStatusBarComp extends SimpleComp {

    BrowserFileSystemTabModel model;

    @Override
    protected Region createSimple() {
        var bar = new HorizontalComp(List.of(
                createProgressNameStatus(),
                createProgressStatus(),
                createProgressEstimateStatus(),
                Comp.hspacer(),
                createClipboardStatus(),
                createSelectionStatus(),
                createKillButton()));
        bar.spacing(15);
        bar.styleClass("status-bar");

        var r = bar.createRegion();
        r.setOnDragDetected(event -> {
            event.consume();
            r.startFullDrag();
        });
        AppFontSizes.xs(r);
        simulateEmptyCell(r);
        return r;
    }

    private Comp<?> createKillButton() {
        var button = new IconButtonComp("mdi2s-stop", () -> {
            ThreadHelper.runAsync(() -> {
                model.killTransfer();
            });
        });
        button.accessibleText("Kill").tooltipKey("killTransfer");
        var cancel = PlatformThread.sync(model.getTransferCancelled());
        var hide = Bindings.createBooleanBinding(
                () -> {
                    if (model.getProgress().getValue() == null) {
                        return true;
                    }

                    if (cancel.getValue()) {
                        return true;
                    }

                    return false;
                },
                cancel,
                model.getProgress());
        button.hide(hide);
        return button;
    }

    private Comp<?> createProgressEstimateStatus() {
        var text = BindingsHelper.map(model.getProgress(), p -> {
            if (p == null) {
                return null;
            } else {
                var expected = p.expectedTimeRemaining();
                var show = p.elapsedTime().compareTo(Duration.of(200, ChronoUnit.MILLIS)) > 0
                        && (!p.hasKnownTotalSize() || p.getTotal() > 50_000_000 || expected.toMillis() > 5000);
                var time = show ? HumanReadableFormat.duration(p.expectedTimeRemaining()) : "";
                return time;
            }
        });
        var progressComp = new LabelComp(text)
                .styleClass("progress")
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .prefWidth(90)
                .minWidth(Region.USE_PREF_SIZE);
        return progressComp;
    }

    private Comp<?> createProgressStatus() {
        var text = BindingsHelper.map(model.getProgress(), p -> {
            if (p == null) {
                return null;
            } else {
                var transferred = HumanReadableFormat.progressByteCount(p.getTransferred());
                if (!p.hasKnownTotalSize()) {
                    return transferred;
                }

                var all = HumanReadableFormat.byteCount(p.getTotal());
                return transferred + " / " + all;
            }
        });
        var progressComp = new LabelComp(text)
                .styleClass("progress")
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .prefWidth(150)
                .minWidth(Region.USE_PREF_SIZE);
        return progressComp;
    }

    private Comp<?> createProgressNameStatus() {
        var text = BindingsHelper.map(model.getProgress(), p -> {
            if (p == null) {
                return null;
            } else {
                return p.getName();
            }
        });
        var progressComp = new LabelComp(text)
                .styleClass("progress")
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .hgrow();
        return progressComp;
    }

    private Comp<?> createClipboardStatus() {
        var cc = BrowserClipboard.currentCopyClipboard;
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
        return new LabelComp(ccCount);
    }

    private Comp<?> createSelectionStatus() {
        var selectedCount = Bindings.createIntegerBinding(
                () -> {
                    return model.getFileList().getSelection().size();
                },
                model.getFileList().getSelection());

        var allCount = Bindings.createIntegerBinding(
                () -> {
                    return model.getFileList().getAll().getValue().size();
                },
                model.getFileList().getAll());
        var selectedComp = new LabelComp(Bindings.createStringBinding(
                () -> {
                    if (selectedCount.getValue() == 0) {
                        return null;
                    } else {
                        return selectedCount.getValue() + " / " + allCount.getValue() + " selected";
                    }
                },
                selectedCount,
                allCount));
        return selectedComp;
    }

    private void simulateEmptyCell(Region r) {
        var emptyEntry = new BrowserFileListCompEntry(null, r, null, model.getFileList());
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
        r.setOnDragDone(event -> {
            emptyEntry.onDragDone(event);
        });

        // Use status bar as an extension of file list
        new ContextMenuAugment<>(
                        mouseEvent -> mouseEvent.getButton() == MouseButton.SECONDARY,
                        null,
                        () -> new BrowserContextMenu(model, null, false))
                .augment(new SimpleCompStructure<>(r));
    }
}
