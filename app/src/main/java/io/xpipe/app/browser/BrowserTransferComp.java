package io.xpipe.app.browser;

import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.DragPseudoClassAugment;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.List;

public class BrowserTransferComp extends SimpleComp {

    private final BrowserTransferModel stage;

    public BrowserTransferComp(BrowserTransferModel stage) {
        this.stage = stage;
    }

    @Override
    protected Region createSimple() {
        var background = new LabelComp(AppI18n.observable("download"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2d-download-outline")))
                .visible(BindingsHelper.persist(Bindings.isEmpty(stage.getItems())));
        var backgroundStack =
                new StackComp(List.of(background)).grow(true, true).styleClass("download-background");

        var binding = BindingsHelper.mappedContentBinding(stage.getItems(), item -> item.getFileEntry());
        var list = new BrowserSelectionListComp(binding).apply(struc -> struc.get().setMinHeight(150)).grow(false, true);
        var dragNotice = new LabelComp(AppI18n.observable("dragFiles"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2e-export")))
                .hide(BindingsHelper.persist(Bindings.isEmpty(stage.getItems())))
                .grow(true, false)
                .apply(struc -> struc.get().setPadding(new Insets(8)));

        var clearButton = new IconButtonComp("mdi2d-delete", () -> {
                    stage.getItems().clear();
                })
                .hide(BindingsHelper.persist(Bindings.isEmpty(stage.getItems())));
        var clearPane = Comp.derive(clearButton, button -> {
            var p = new AnchorPane(button);
            AnchorPane.setRightAnchor(button, 10.0);
            AnchorPane.setTopAnchor(button, 10.0);
            return p;
        });

        var listBox = new VerticalComp(List.of(list, dragNotice));
        var stack = new LoadingOverlayComp(
                new StackComp(List.of(backgroundStack, listBox, clearPane)).apply(DragPseudoClassAugment.create()).apply(struc -> {
                    struc.get().setOnDragOver(event -> {
                        // Accept drops from inside the app window
                        if (event.getGestureSource() != null && event.getGestureSource() != struc.get()) {
                            event.acceptTransferModes(TransferMode.ANY);
                            event.consume();
                        }
                    });
                    struc.get().setOnDragDropped(event -> {
                        if (event.getGestureSource() != null) {
                            var files = BrowserClipboard.retrieveDrag(event.getDragboard())
                                    .getEntries();
                            stage.drop(files);
                            event.setDropCompleted(true);
                            event.consume();
                        }
                    });
                    struc.get().setOnDragDetected(event -> {
                        if (stage.getDownloading().get()) {
                            return;
                        }

                        var files = stage.getItems().stream()
                                .map(item -> {
                                    try {
                                        return item.getLocalFile().toRealPath().toFile();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .toList();
                        Dragboard db = struc.get().startDragAndDrop(TransferMode.MOVE);
                        var cc = new ClipboardContent();
                        cc.putFiles(files);
                        db.setContent(cc);

                        var image = BrowserSelectionListComp.snapshot(FXCollections.observableList(stage.getItems().stream()
                                        .map(item -> item.getFileEntry())
                                        .toList()));
                        db.setDragView(image, -20, 15);

                        event.setDragDetect(true);
                        event.consume();
                    });
                    struc.get().setOnDragDone(event -> {
                        if (!event.isAccepted()) {
                            return;
                        }

                        stage.getItems().clear();
                        event.consume();
                    });
                }),
                PlatformThread.sync(stage.getDownloading()));
        return stack.createRegion();
    }
}
