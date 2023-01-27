package io.xpipe.app.comp.storage.source;

import io.xpipe.app.comp.base.LazyTextFieldComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.comp.source.DsDataTransferComp;
import io.xpipe.app.comp.storage.DataSourceTypeComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.core.store.DataFlow;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.augment.GrowAugment;
import io.xpipe.extension.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.extension.fxcomps.impl.IconButtonComp;
import io.xpipe.extension.fxcomps.impl.LabelComp;
import io.xpipe.extension.fxcomps.impl.PrettyImageComp;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import lombok.SneakyThrows;

public class SourceEntryComp extends SimpleComp {

    private static final double SOURCE_TYPE_WIDTH = 0.09;
    private static final double NAME_WIDTH = 0.3;
    private static final double DETAILS_WIDTH = 0.43;
    private static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    private static final PseudoClass INCOMPLETE = PseudoClass.getPseudoClass("incomplete");
    private static Image DND_IMAGE = null;
    private final SourceEntryWrapper entry;

    public SourceEntryComp(SourceEntryWrapper entry) {
        this.entry = entry;
    }

    private Label createSize() {
        var size = new Label();
        size.textProperty().bind(PlatformThread.sync(entry.getStoreSummary()));
        size.getStyleClass().add("size");
        AppFont.small(size);
        return size;
    }

    private LazyTextFieldComp createName() {
        var name = new LazyTextFieldComp(entry.getName());
        name.apply(s -> AppFont.header(s.get()));
        return name;
    }

    private void applyState(Node node) {
        SimpleChangeListener.apply(PlatformThread.sync(entry.getState()), val -> {
            switch (val) {
                case LOAD_FAILED -> {
                    node.pseudoClassStateChanged(FAILED, true);
                    node.pseudoClassStateChanged(INCOMPLETE, false);
                }
                case INCOMPLETE -> {
                    node.pseudoClassStateChanged(FAILED, false);
                    node.pseudoClassStateChanged(INCOMPLETE, true);
                }
                default -> {
                    node.pseudoClassStateChanged(FAILED, false);
                    node.pseudoClassStateChanged(INCOMPLETE, false);
                }
            }
        });
    }

    @SneakyThrows
    @Override
    protected Region createSimple() {
        var loading = new LoadingOverlayComp(Comp.of(() -> createContent()), entry.getLoading());
        var region = loading.createRegion();
        return region;
    }

    protected Region createContent() {
        var name = createName().createRegion();

        var size = createSize();
        var img = entry.getState().getValue() == DataSourceEntry.State.LOAD_FAILED
                ? "disabled_icon.png"
                : entry.getEntry().getProvider().getDisplayIconFileName();
        var storeIcon = new PrettyImageComp(new SimpleStringProperty(img), 60, 50).createRegion();

        var desc = new LabelComp(entry.getInformation()).createRegion();
        desc.getStyleClass().add("description");
        AppFont.header(desc);

        var date = new Label();
        date.textProperty().bind(AppI18n.readableDuration("usedDate", PlatformThread.sync(entry.getLastUsed())));
        date.getStyleClass().add("date");
        AppFont.small(date);

        var grid = new GridPane();
        grid.getColumnConstraints()
                .addAll(
                        createShareConstraint(grid, SOURCE_TYPE_WIDTH),
                        createShareConstraint(grid, NAME_WIDTH),
                        new ColumnConstraints(-1));

        var typeLogo = new DataSourceTypeComp(
                        entry.getEntry().getDataSourceType(),
                        entry.getDataFlow().getValue())
                .createRegion();
        typeLogo.maxWidthProperty().bind(typeLogo.heightProperty());

        grid.add(typeLogo, 0, 0, 1, 2);
        GridPane.setHalignment(typeLogo, HPos.CENTER);
        grid.add(name, 1, 0);
        grid.add(date, 1, 1);
        grid.add(storeIcon, 2, 0, 1, 2);
        grid.add(size, 3, 1);
        grid.add(desc, 3, 0);
        grid.setVgap(5);

        AppFont.small(size);
        AppFont.small(date);

        grid.prefHeightProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            return name.getHeight() + date.getHeight() + 5;
                        },
                        name.heightProperty(),
                        date.heightProperty()));

        grid.getStyleClass().add("content");

        grid.setMaxHeight(100);
        grid.setHgap(8);

        var buttons = new HBox();
        buttons.setFillHeight(true);
        buttons.getChildren().add(createPipeButton().createRegion());
        //         buttons.getChildren().add(createUpdateButton().createRegion());
        buttons.getChildren().add(createSettingsButton(name).createRegion());
        buttons.setMinWidth(Region.USE_PREF_SIZE);

        var hbox = new HBox(grid, buttons);
        hbox.getStyleClass().add("storage-entry-comp");
        HBox.setHgrow(grid, Priority.ALWAYS);
        buttons.prefHeightProperty().bind(hbox.heightProperty());

        hbox.getProperties().put("entry", this.entry);
        hbox.setOnDragDetected(e -> {
            if (!entry.getUsable().get()) {
                return;
            }

            if (DND_IMAGE == null) {
                var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, "img/file_drag_icon.png")
                        .orElseThrow();
                DND_IMAGE = new Image(url.toString(), 80, 80, true, false);
            }

            Dragboard db = hbox.startDragAndDrop(TransferMode.MOVE);
            var cc = new ClipboardContent();
            cc.putString("");
            db.setContent(cc);
            db.setDragView(DND_IMAGE, 30, 60);
            e.consume();
        });

        applyState(hbox);

        return hbox;
    }

    private Comp<?> createSettingsButton(Region nameField) {
        var settingsButton = new IconButtonComp("mdi2v-view-headline");
        settingsButton.styleClass("settings");
        settingsButton.apply(new SourceEntryContextMenu<>(true, entry, nameField));
        settingsButton.apply(GrowAugment.create(false, true));
        settingsButton.apply(s -> {
            s.get().prefWidthProperty().bind(Bindings.divide(s.get().heightProperty(), 1.35));
        });
        settingsButton.apply(new FancyTooltipAugment<>("entrySettings"));
        return settingsButton;
    }

    private Comp<?> createPipeButton() {
        var pipeButton = new IconButtonComp("mdi2p-pipe-disconnected", () -> {
            DsDataTransferComp.showPipeWindow(this.entry.getEntry());
        });
        pipeButton.styleClass("retrieve");
        pipeButton.apply(GrowAugment.create(false, true));
        pipeButton.apply(s -> {
            s.get().prefWidthProperty().bind(Bindings.divide(s.get().heightProperty(), 1.35));
        });
        var disabled = Bindings.createBooleanBinding(
                () -> {
                    if (entry.getDataFlow().getValue() == null) {
                        return true;
                    }

                    return entry.getDataFlow().getValue() == DataFlow.OUTPUT
                            || entry.getDataFlow().getValue() == DataFlow.TRANSFORMER;
                },
                entry.getDataFlow());
        pipeButton.disable(disabled).apply(s -> s.get());
        pipeButton.apply(new FancyTooltipAugment<>("retrieve"));
        return pipeButton;
    }

    private ColumnConstraints createShareConstraint(Region r, double share) {
        var cc = new ColumnConstraints();
        cc.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> r.getWidth() * share, r.widthProperty()));
        cc.setMaxWidth(750 * share);
        return cc;
    }
}
