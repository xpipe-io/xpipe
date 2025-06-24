package io.xpipe.app.browser.file;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.DerivedObservableList;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;

import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class BrowserTransferComp extends SimpleComp {

    private final BrowserTransferModel model;

    public BrowserTransferComp(BrowserTransferModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var background = new LabelComp(AppI18n.observable("transferDescription"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2d-download-outline")))
                .apply(struc -> struc.get().setWrapText(true))
                .apply(struc -> struc.get().setTextAlignment(TextAlignment.CENTER))
                .apply(struc -> struc.get().setContentDisplay(ContentDisplay.TOP))
                .visible(model.getEmpty());
        var backgroundStack = new StackComp(List.of(background))
                .grow(true, true)
                .styleClass("color-box")
                .styleClass("gray")
                .styleClass("download-background");

        var binding = DerivedObservableList.wrap(model.getItems(), true)
                .mapped(item -> item.getBrowserEntry())
                .getList();
        var list = new BrowserFileSelectionListComp(binding, entry -> {
                    var sourceItem = model.getCurrentItems().stream()
                            .filter(item -> item.getBrowserEntry() == entry)
                            .findAny();
                    if (sourceItem.isEmpty()) {
                        return new SimpleStringProperty("?");
                    }
                    synchronized (sourceItem.get().getProgress()) {
                        return Bindings.createStringBinding(
                                () -> {
                                    var p = sourceItem.get().getProgress().getValue();
                                    if (p == null || !p.hasKnownTotalSize()) {
                                        return entry.getFileName();
                                    }

                                    var hideProgress = sourceItem
                                            .get()
                                            .getDownloadFinished()
                                            .get();
                                    var share = p.getTransferred() * 100 / p.getTotal();
                                    var progressSuffix = hideProgress ? "" : " " + share + "%";
                                    return entry.getFileName() + progressSuffix;
                                },
                                sourceItem.get().getProgress());
                    }
                })
                .grow(false, true);
        var dragNotice = new LabelComp(AppI18n.observable("dragLocalFiles"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2h-hand-left")))
                .apply(struc -> struc.get().setWrapText(true))
                .hide(Bindings.or(model.getEmpty(), model.getTransferring()));

        var clearButton = new IconButtonComp("mdi2c-close", () -> {
                    ThreadHelper.runAsync(() -> {
                        model.clear(true);
                    });
                })
                .hide(Bindings.or(model.getEmpty(), model.getTransferring()))
                .tooltipKey("clearTransferDescription");

        var downloadButton = new IconButtonComp("mdi2f-folder-move-outline", () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        model.transferToDownloads();
                    });
                })
                .hide(Bindings.or(model.getEmpty(), model.getTransferring()))
                .tooltipKey("downloadStageDescription");

        var bottom = new HorizontalComp(
                List.of(Comp.hspacer(), dragNotice, Comp.hspacer(), downloadButton, Comp.hspacer(4), clearButton));
        var listBox = new VerticalComp(List.of(list, bottom))
                .spacing(5)
                .padding(new Insets(10, 10, 5, 10))
                .apply(struc -> struc.get().setMinHeight(200))
                .apply(struc -> struc.get().setMaxHeight(200));
        var stack = new StackComp(List.of(backgroundStack, listBox)).apply(struc -> {
            struc.get().addEventFilter(DragEvent.DRAG_ENTERED, event -> {
                struc.get().pseudoClassStateChanged(PseudoClass.getPseudoClass("drag-over"), true);
            });
            struc.get().addEventFilter(DragEvent.DRAG_EXITED, event -> struc.get()
                    .pseudoClassStateChanged(PseudoClass.getPseudoClass("drag-over"), false));
            struc.get().setOnDragOver(event -> {
                // Accept drops from inside the app window
                if (event.getGestureSource() != null && event.getGestureSource() != struc.get()) {
                    event.acceptTransferModes(TransferMode.ANY);
                    event.consume();
                }
            });
            struc.get().setOnDragDropped(event -> {
                // Accept drops from inside the app window
                if (event.getGestureSource() != null) {
                    var drag = BrowserClipboard.retrieveDrag(event.getDragboard());
                    if (drag == null) {
                        return;
                    }

                    if (!(model.getBrowserSessionModel().getSelectedEntry().getValue()
                            instanceof BrowserFileSystemTabModel fileSystemModel)) {
                        return;
                    }

                    var files = drag.getEntries();
                    model.drop(fileSystemModel, files);
                    event.setDropCompleted(true);
                    event.consume();
                }
            });
            struc.get().setOnDragDetected(event -> {
                var items = model.getCurrentItems();
                var selected =
                        items.stream().map(item -> item.getBrowserEntry()).toList();
                var files = items.stream()
                        .filter(item -> item.getDownloadFinished().get())
                        .map(item -> {
                            try {
                                var file = item.getLocalFile();
                                if (!Files.exists(file)) {
                                    return Optional.<File>empty();
                                }

                                return Optional.of(file.toRealPath().toFile());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .flatMap(Optional::stream)
                        .toList();
                if (files.isEmpty()) {
                    return;
                }

                var cc = new ClipboardContent();
                cc.putFiles(files);
                Dragboard db = struc.get().startDragAndDrop(TransferMode.COPY);
                db.setContent(cc);

                Image image = BrowserFileSelectionListComp.snapshot(FXCollections.observableList(selected));
                db.setDragView(image, -20, 15);

                event.setDragDetect(true);
                event.consume();
            });
            struc.get().setOnDragDone(event -> {
                if (!event.isAccepted()) {
                    return;
                }

                // The files might not have been transferred yet
                // We can't listen to this, so just don't delete them
                model.clear(false);
                event.consume();
            });
        });

        stack.apply(struc -> {
            model.getBrowserSessionModel().getDraggingFiles().addListener((observable, oldValue, newValue) -> {
                struc.get().pseudoClassStateChanged(PseudoClass.getPseudoClass("highlighted"), newValue);
            });
        });

        var r = stack.styleClass("transfer").createRegion();
        return r;
    }
}
