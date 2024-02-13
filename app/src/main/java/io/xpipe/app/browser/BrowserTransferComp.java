package io.xpipe.app.browser;

import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.DragOverPseudoClassAugment;
import io.xpipe.app.fxcomps.impl.*;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileNames;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
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
        var background = new LabelComp(AppI18n.observable("transferDescription"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2d-download-outline")))
                .visible(BindingsHelper.persist(Bindings.isEmpty(model.getItems())));
        var backgroundStack =
                new StackComp(List.of(background)).grow(true, true).styleClass("download-background");

        var binding = BindingsHelper.mappedContentBinding(model.getItems(), item -> item.getFileEntry());
        var list = new BrowserSelectionListComp(binding, entry -> Bindings.createStringBinding(() -> {
            var sourceItem = model.getItems().stream().filter(item -> item.getFileEntry() == entry).findAny();
            if (sourceItem.isEmpty()) {
                return "?";
            }
            var name = sourceItem.get().downloadFinished().get() ? "Local" : DataStorage.get().getStoreDisplayName(entry.getFileSystem().getStore()).orElse("?");
            return FileNames.getFileName(entry.getPath()) + " (" + name + ")";
        }, model.getAllDownloaded()))
                .apply(struc -> struc.get().setMinHeight(150))
                .grow(false, true);
        var dragNotice = new LabelComp(model.getAllDownloaded().flatMap(aBoolean -> aBoolean ? AppI18n.observable("dragLocalFiles") : AppI18n.observable("dragFiles")))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2e-export")))
                .hide(PlatformThread.sync(
                        BindingsHelper.persist(Bindings.isEmpty(model.getItems()))))
                .grow(true, false)
                .apply(struc -> struc.get().setPadding(new Insets(8)));

        var downloadButton = new IconButtonComp("mdi2d-download", () -> {
                    model.download();
                })
                .hide(BindingsHelper.persist(Bindings.isEmpty(model.getItems())))
                .disable(PlatformThread.sync(model.getAllDownloaded()))
                .apply(new FancyTooltipAugment<>("downloadStageDescription"));
        var clearButton = new IconButtonComp("mdi2c-close", () -> {
                    model.clear();
                })
                .hide(BindingsHelper.persist(Bindings.isEmpty(model.getItems())));
        var clearPane = Comp.derive(
                new HorizontalComp(List.of(downloadButton, clearButton))
                        .apply(struc -> struc.get().setSpacing(10)),
                button -> {
                    var p = new AnchorPane(button);
                    AnchorPane.setRightAnchor(button, 10.0);
                    AnchorPane.setTopAnchor(button, 10.0);
                    p.setPickOnBounds(false);
                    return p;
                });

        var listBox = new VerticalComp(List.of(list, dragNotice)).padding(new Insets(10, 10, 5, 10));
        var stack = LoadingOverlayComp.noProgress(
                new StackComp(List.of(backgroundStack, listBox, clearPane))
                        .apply(DragOverPseudoClassAugment.create())
                        .apply(struc -> {
                            struc.get().setOnDragOver(event -> {
                                // Accept drops from inside the app window
                                if (event.getGestureSource() != null && event.getGestureSource() != struc.get()) {
                                    event.acceptTransferModes(TransferMode.ANY);
                                    event.consume();
                                }

                                // Accept drops from outside the app window
                                if (event.getGestureSource() == null && !event.getDragboard().getFiles().isEmpty()) {
                                    event.acceptTransferModes(TransferMode.ANY);
                                    event.consume();
                                }
                            });
                            struc.get().setOnDragDropped(event -> {
                                // Accept drops from inside the app window
                                if (event.getGestureSource() != null) {
                                    var files = BrowserClipboard.retrieveDrag(event.getDragboard())
                                            .getEntries();
                                    model.drop(model.getBrowserModel().getSelected().getValue(), files);
                                    event.setDropCompleted(true);
                                    event.consume();
                                }

                                // Accept drops from outside the app window
                                if (event.getGestureSource() == null) {
                                    model.dropLocal(event.getDragboard().getFiles());
                                    event.setDropCompleted(true);
                                    event.consume();
                                }
                            });
                            struc.get().setOnDragDetected(event -> {
                                if (model.getDownloading().get()) {
                                    return;
                                }

                                var selected = model.getItems().stream().map(BrowserTransferModel.Item::getFileEntry).toList();
                                Dragboard db = struc.get().startDragAndDrop(TransferMode.COPY);

                                var cc = BrowserClipboard.startDrag(null, selected);
                                if (cc == null) {
                                    return;
                                }

                                var files = model.getItems().stream()
                                        .filter(item -> item.downloadFinished().get())
                                        .map(item -> {
                                            try {
                                                var file = item.getLocalFile();
                                                if (!Files.exists(file)) {
                                                    return Optional.<File>empty();
                                                }

                                                return Optional.of(file
                                                        .toRealPath()
                                                        .toFile());
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
                                // macOS does always report false here, which is unfortunate
                                if (!event.isAccepted() && !OsType.getLocal().equals(OsType.MACOS)) {
                                    return;
                                }

                                model.clear();
                                event.consume();
                            });
                        }),
                PlatformThread.sync(model.getDownloading()));
        return stack.styleClass("transfer").createRegion();
    }
}
