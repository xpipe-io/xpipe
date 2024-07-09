package io.xpipe.app.browser;

import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.DragOverPseudoClassAugment;
import io.xpipe.app.fxcomps.impl.*;
import io.xpipe.app.fxcomps.util.DerivedObservableList;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
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
        var syncItems = PlatformThread.sync(model.getItems());

        var background = new LabelComp(AppI18n.observable("transferDescription"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2d-download-outline")))
                .apply(struc -> struc.get().setWrapText(true))
                .visible(Bindings.isEmpty(syncItems));
        var backgroundStack =
                new StackComp(List.of(background)).grow(true, true).styleClass("download-background");

        var binding = new DerivedObservableList<>(syncItems, true)
                .mapped(item -> item.getBrowserEntry())
                .getList();
        var list = new BrowserSelectionListComp(
                        binding,
                        entry -> {
                            var sourceItem = syncItems.stream()
                                    .filter(item -> item.getBrowserEntry() == entry)
                                    .findAny();
                            if (sourceItem.isEmpty()) {
                                return new SimpleStringProperty("?");
                            }
                            return Bindings.createStringBinding(() -> {
                                var p = sourceItem.get().getProgress().getValue();
                                var progressSuffix = sourceItem.get().downloadFinished().get() ? "" : " " + (p.getTransferred() * 100 / p.getTotal()) + "%";
                                return entry.getFileName() + progressSuffix;
                            }, sourceItem.get().getProgress());
                        })
                .grow(false, true);
        var dragNotice = new LabelComp(AppI18n.observable("dragLocalFiles"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2h-hand-left")))
                .apply(struc -> AppFont.medium(struc.get()))
                .apply(struc -> struc.get().setWrapText(true))
                .hide(Bindings.isEmpty(syncItems));

        var clearButton = new IconButtonComp("mdi2c-close", () -> {
            ThreadHelper.runAsync(() -> {
                model.clear(true);
            });
                })
                .hide(Bindings.isEmpty(syncItems))
                .tooltipKey("clearTransferDescription");

        var downloadButton = new IconButtonComp("mdi2f-folder-move-outline", () -> {
            ThreadHelper.runFailableAsync(() -> {
                model.transferToDownloads();
            });
        })
                .hide(Bindings.isEmpty(syncItems))
                .tooltipKey("downloadStageDescription");

        var bottom =
                new HorizontalComp(List.of(Comp.hspacer(), dragNotice, Comp.hspacer(), downloadButton, Comp.hspacer(4), clearButton));
        var listBox = new VerticalComp(List.of(list, bottom))
                .spacing(5)
                .padding(new Insets(10, 10, 5, 10))
                .apply(struc -> struc.get().setMinHeight(200))
                .apply(struc -> struc.get().setMaxHeight(200));
        var stack = new StackComp(List.of(backgroundStack, listBox))
                        .apply(DragOverPseudoClassAugment.create())
                        .apply(struc -> {
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

                                    if (!(model.getBrowserSessionModel()
                                                    .getSelectedEntry()
                                                    .getValue()
                                            instanceof OpenFileSystemModel fileSystemModel)) {
                                        return;
                                    }

                                    var files = drag.getEntries();
                                    model.drop(fileSystemModel, files);
                                    event.setDropCompleted(true);
                                    event.consume();
                                }
                            });
                            struc.get().setOnDragDetected(event -> {
                                var selected = syncItems.stream()
                                        .map(item -> item.getBrowserEntry())
                                        .toList();
                                Dragboard db = struc.get().startDragAndDrop(TransferMode.COPY);

                                var cc = new ClipboardContent();
                                var files = syncItems.stream()
                                        .filter(item -> item.downloadFinished().get())
                                        .map(item -> {
                                            try {
                                                var file = item.getLocalFile();
                                                if (!Files.exists(file)) {
                                                    return Optional.<File>empty();
                                                }

                                                return Optional.of(
                                                        file.toRealPath().toFile());
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        })
                                        .flatMap(Optional::stream)
                                        .toList();
                                cc.putFiles(files);
                                db.setContent(cc);

                                Image image = BrowserSelectionListComp.snapshot(FXCollections.observableList(selected));
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
                struc.get().pseudoClassStateChanged(PseudoClass.getPseudoClass("highlighted"),newValue);
            });
        });

        return stack.styleClass("transfer").createRegion();
    }
}
