package io.xpipe.app.hub.entry;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.Listeners;
import io.xpipe.app.platform.PlatformThread;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.List;

public class StoreEntryInformationComp extends SimpleRegionBuilder {

    private final StoreEntryWrapper wrapper;
    private final ObservableValue<StoreEntryInformation> value;

    public StoreEntryInformationComp(StoreEntryWrapper wrapper, ObservableValue<StoreEntryInformation> value) {
        this.wrapper = wrapper;
        this.value = value;
    }

    @Override
    protected Region createSimple() {
        var l = DerivedObservableList.<StoreEntryBadge>arrayList(true);
        value.subscribe(storeEntryInformation -> {
            l.setContent(
                    storeEntryInformation != null
                            ? storeEntryInformation.getBadges().stream()
                                    .filter(storeEntryBadge -> {
                                        return storeEntryBadge.getName() != null
                                                || storeEntryBadge.getGraphic() != null;
                                    })
                                    .toList()
                            : List.of());
        });

        var h = new HorizontalComp(List.of());
        h.spacing(8);

        var hbox = h.build();
        hbox.setFillHeight(true);
        hbox.setMinWidth(0);
        hbox.setPrefWidth(Region.USE_COMPUTED_SIZE);
        hbox.setAlignment(Pos.CENTER_LEFT);

        var breakpoint = new SimpleDoubleProperty();
        var compress = new SimpleBooleanProperty();

        for (StoreEntryBadge badge : l.getList()) {
            var comp = createBadge(badge, compress.get());
            if (comp != null) {
                var r = comp.build();
                hbox.getChildren().add(r);
            }
        }

        Listeners.attachWithScene(hbox, hbox.widthProperty(), val -> {
            fill(hbox, l.getList(), compress, breakpoint, false, false);
        });

        l.getList().addListener((ListChangeListener<? super StoreEntryBadge>) c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                fill(hbox, l.getList(), compress, breakpoint, true, false);
            });
        });

        return hbox;
    }

    private void fill(
            HBox hbox,
            List<StoreEntryBadge> badges,
            BooleanProperty compress,
            DoubleProperty breakpoint,
            boolean rebuildFull,
            boolean rebuildFromCompress) {
        var wasCompress = compress.get();

        if (rebuildFull || rebuildFromCompress) {
            if (rebuildFull) {
                compress.set(false);
            }

            hbox.getChildren().clear();
            for (StoreEntryBadge badge : badges) {
                var comp = createBadge(badge, compress.get());
                if (comp != null) {
                    var r = comp.build();
                    hbox.getChildren().add(r);
                }
            }
        }

        var layouted = hbox.getWidth() > 0.0;

        double maxX = 0.0;
        for (var child : hbox.getChildren()) {
            Region childRegion = (Region) child;
            maxX = childRegion.localToParent(childRegion.getBoundsInLocal()).getMaxX();
            childRegion.setVisible(hbox.getWidth() > 0.0 && maxX < hbox.getWidth());
            layouted = layouted && childRegion.getWidth() > 10.0;
        }

        if (layouted) {
            if (!compress.get()) {
                breakpoint.set(maxX);
            }

            compress.set(hbox.getWidth() < breakpoint.get());
            if (wasCompress != compress.get()) {
                fill(hbox, badges, compress, breakpoint, false, true);
            }
        } else if (hbox.getChildren().size() > 0) {
            Platform.runLater(() -> {
                fill(hbox, badges, compress, breakpoint, false, false);
            });
        }
    }

    private BaseRegionBuilder<?, ?> createBadge(StoreEntryBadge val, boolean compress) {
        if (compress && val.getCompressBehaviour() == StoreEntryBadge.CompressBehaviour.HIDE) {
            return null;
        }

        var name = compress && val.getCompressBehaviour() == StoreEntryBadge.CompressBehaviour.COMPRESS_TO_GRAPHIC
                ? val.getCompressedName().orElse(val.getGraphic() != null ? null : val.getName())
                : val.getName();
        var button = new ButtonComp(new ReadOnlyObjectWrapper<>(name), val.getGraphic(), null);
        button.maxHeight(100);
        button.maxWidth(400);
        button.style("store-entry-badge");
        val.getAction()
                .filter(action -> action.checkApplicable(wrapper))
                .ifPresentOrElse(
                        action -> {
                            button.apply(b -> {
                                b.setOnMouseClicked(event -> {
                                    if (event.getButton() != MouseButton.PRIMARY) {
                                        return;
                                    }

                                    action.run(wrapper, b);
                                    event.consume();
                                });
                            });
                        },
                        () -> {
                            button.disable(true);
                        });
        if (val.getGraphic() instanceof LabelGraphic.ImageGraphic) {
            button.apply(b -> b.setGraphicTextGap(7));
        }
        val.getStyleClass().ifPresent(button::style);
        return button;
    }
}
