package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.update.UpdateAvailableDialog;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class SideMenuBarComp extends Comp<CompStructure<VBox>> {

    private final Property<AppLayoutModel.Entry> value;
    private final List<AppLayoutModel.Entry> entries;
    private final ObservableList<AppLayoutModel.QueueEntry> queueEntries;

    @Override
    public CompStructure<VBox> createBase() {
        var vbox = new VBox();
        vbox.setFillWidth(true);

        for (AppLayoutModel.Entry e : entries) {
            var b = new IconButtonComp(e.icon(), () -> {
                if (e.action() != null) {
                    e.action().run();
                    return;
                }

                value.setValue(e);
            });
            b.tooltip(e.name());
            b.accessibleText(e.name());

            var stack = createStyle(e, b);
            var shortcut = e.combination();
            if (shortcut != null) {
                stack.apply(struc -> struc.get().getProperties().put("shortcut", shortcut));
            }
            vbox.getChildren().add(stack.createRegion());
        }

        {
            var b = new IconButtonComp("mdi2u-update", () -> UpdateAvailableDialog.showIfNeeded(false));
            b.tooltipKey("updateAvailableTooltip").accessibleTextKey("updateAvailableTooltip");
            var stack = createStyle(null, b);
            stack.hide(Bindings.createBooleanBinding(
                    () -> {
                        return AppDistributionType.get()
                                        .getUpdateHandler()
                                        .getPreparedUpdate()
                                        .getValue()
                                == null;
                    },
                    AppDistributionType.get().getUpdateHandler().getPreparedUpdate()));
            vbox.getChildren().add(stack.createRegion());
        }

        if (!AppProperties.get().isStaging()) {
            var b = new IconButtonComp("mdoal-insights", () -> Hyperlinks.open(Hyperlinks.GITHUB_PTB));
            b.tooltipKey("ptbAvailableTooltip");
            b.accessibleTextKey("ptbAvailableTooltip");
            var stack = createStyle(null, b);
            stack.hide(AppLayoutModel.get().getPtbAvailable().not());
            vbox.getChildren().add(stack.createRegion());
        }

        var filler = new Button();
        filler.setDisable(true);
        filler.setMaxHeight(3000);
        vbox.getChildren().add(filler);
        VBox.setVgrow(filler, Priority.ALWAYS);
        vbox.getStyleClass().add("sidebar-comp");

        var queueButtons = new VBox();
        queueEntries.addListener((ListChangeListener<? super AppLayoutModel.QueueEntry>) c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                queueButtons.getChildren().clear();
                for (int i = c.getList().size() - 1; i >= 0; i--) {
                    var item = c.getList().get(i);
                    var b = new IconButtonComp(item.getIcon(), null);
                    b.apply(struc -> {
                        var tt = TooltipHelper.create(item.getName(), null);
                        tt.setShowDelay(Duration.millis(50));
                        Tooltip.install(struc.get(), tt);

                        struc.get().setOnAction(e -> {
                            struc.get().setDisable(true);
                            ThreadHelper.runAsync(() -> {
                                try {
                                    item.getAction().run();
                                } finally {
                                    Platform.runLater(() -> {
                                        queueEntries.remove(item);
                                    });
                                }
                            });
                            e.consume();
                        });
                    });
                    b.accessibleText(item.getName());
                    var stack = createStyle(null, b);
                    queueButtons.getChildren().add(stack.createRegion());
                }
            });
        });
        vbox.getChildren().add(queueButtons);
        vbox.setMinHeight(0);
        vbox.setPrefHeight(0);

        return new SimpleCompStructure<>(vbox);
    }

    private Comp<?> createStyle(AppLayoutModel.Entry e, IconButtonComp b) {
        var selected = PseudoClass.getPseudoClass("selected");

        b.apply(struc -> {
            AppFontSizes.lg(struc.get());
            struc.get().setAlignment(Pos.CENTER);

            struc.get().pseudoClassStateChanged(selected, value.getValue().equals(e));
            value.addListener((c, o, n) -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    struc.get().pseudoClassStateChanged(selected, n.equals(e));
                });
            });
        });

        var selectedBorder = Bindings.createObjectBinding(
                () -> {
                    var c = Platform.getPreferences()
                            .getAccentColor()
                            .desaturate()
                            .desaturate();
                    return new Background(new BackgroundFill(c, new CornerRadii(8), new Insets(17, 1, 15, 2)));
                },
                Platform.getPreferences().accentColorProperty());
        var hoverBorder = Bindings.createObjectBinding(
                () -> {
                    var c = Platform.getPreferences()
                            .getAccentColor()
                            .darker()
                            .desaturate()
                            .desaturate();
                    return new Background(new BackgroundFill(c, new CornerRadii(8), new Insets(17, 1, 15, 2)));
                },
                Platform.getPreferences().accentColorProperty());
        var noneBorder = Bindings.createObjectBinding(
                () -> {
                    return Background.fill(Color.TRANSPARENT);
                },
                Platform.getPreferences().accentColorProperty());

        var indicator = Comp.empty().styleClass("indicator");
        var stack =
                new StackComp(List.of(indicator, b)).apply(struc -> struc.get().setAlignment(Pos.CENTER_RIGHT));
        stack.apply(struc -> {
            var indicatorRegion = (Region) struc.get().getChildren().getFirst();
            var buttonRegion = (Region) struc.get().getChildren().get(1);
            indicatorRegion.setMaxWidth(7);
            indicatorRegion.prefHeightProperty().bind(buttonRegion.heightProperty());
            indicatorRegion
                    .backgroundProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> {
                                if (value.getValue().equals(e)) {
                                    return selectedBorder.get();
                                }

                                if (struc.get().isHover()) {
                                    return hoverBorder.get();
                                }

                                return noneBorder.get();
                            },
                            struc.get().hoverProperty(),
                            value,
                            hoverBorder,
                            selectedBorder,
                            noneBorder));
        });
        return stack;
    }
}
