package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.Validator;
import io.xpipe.app.util.Hyperlinks;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class OptionsComp extends Comp<CompStructure<VBox>> {

    private final List<Entry> entries;
    private final Validator validator;

    public OptionsComp(List<Entry> entries, Validator validator) {
        this.entries = entries;
        this.validator = validator;
    }

    @Override
    public CompStructure<VBox> createBase() {
        VBox pane = new VBox();
        pane.getStyleClass().add("options-comp");

        var nameRegions = new ArrayList<Region>();

        Region firstComp = null;
        for (var entry : getEntries()) {
            Region compRegion = entry.comp() != null ? entry.comp().createRegion() : new Region();

            if (firstComp == null) {
                compRegion.getStyleClass().add("first");
                firstComp = compRegion;
            }

            var showVertical = (entry.name() != null
                    && (entry.description() != null || entry.comp() instanceof SimpleTitledPaneComp));
            if (showVertical) {
                var line = new VBox();
                line.prefWidthProperty().bind(pane.widthProperty());

                var name = new Label();
                name.getStyleClass().add("name");
                name.textProperty().bind(entry.name());
                name.setMinWidth(Region.USE_PREF_SIZE);
                name.setMinHeight(Region.USE_PREF_SIZE);
                name.setAlignment(Pos.CENTER_LEFT);
                VBox.setVgrow(line, VBox.getVgrow(compRegion));
                line.spacingProperty()
                        .bind(Bindings.createDoubleBinding(
                                () -> {
                                    return name.isManaged() ? 2.0 : 0.0;
                                },
                                name.managedProperty()));
                name.visibleProperty().bind(compRegion.visibleProperty());
                name.managedProperty().bind(compRegion.managedProperty());
                line.getChildren().add(name);
                VBox.setMargin(name, new Insets(0, 0, 0, 1));

                if (entry.description() != null) {
                    var description = new Label();
                    description.setWrapText(true);
                    description.getStyleClass().add("description");
                    description.textProperty().bind(entry.description());
                    description.setAlignment(Pos.CENTER_LEFT);
                    description.setMinHeight(Region.USE_PREF_SIZE);
                    description.visibleProperty().bind(compRegion.visibleProperty());
                    description.managedProperty().bind(compRegion.managedProperty());

                    if (entry.documentationLink() != null) {
                        var link = new Button("... ?");
                        link.setMinWidth(Region.USE_PREF_SIZE);
                        link.getStyleClass().add(Styles.BUTTON_OUTLINED);
                        link.getStyleClass().add(Styles.ACCENT);
                        link.getStyleClass().add("long-description");
                        link.setAccessibleText("Help");
                        AppFontSizes.xl(link);
                        link.setOnAction(e -> {
                            Hyperlinks.open(entry.documentationLink());
                            e.consume();
                        });

                        var tt = TooltipHelper.create(new SimpleStringProperty(entry.documentationLink()), null);
                        tt.setShowDelay(Duration.millis(1));
                        Tooltip.install(link, tt);

                        var descriptionBox =
                                new HBox(description, new Spacer(Orientation.HORIZONTAL), link);
                        descriptionBox.getStyleClass().add("description-box");
                        descriptionBox.setSpacing(5);
                        HBox.setHgrow(descriptionBox, Priority.ALWAYS);
                        descriptionBox.setAlignment(Pos.TOP_LEFT);
                        line.getChildren().add(descriptionBox);
                        VBox.setMargin(descriptionBox, new Insets(0, 0, 0, 1));
                        descriptionBox.visibleProperty().bind(compRegion.visibleProperty());
                        descriptionBox.managedProperty().bind(compRegion.managedProperty());
                    } else {
                        line.getChildren().add(description);
                        line.getChildren().add(new Spacer(2, Orientation.VERTICAL));
                        VBox.setMargin(description, new Insets(0, 0, 0, 1));
                    }
                }

                compRegion.accessibleTextProperty().bind(name.textProperty());
                if (entry.description() != null) {
                    compRegion.accessibleHelpProperty().bind(entry.description());
                }
                line.getChildren().add(compRegion);
                compRegion.getStyleClass().add("options-content");

                pane.getChildren().add(line);
            } else if (entry.name() != null) {
                var line = new HBox();
                line.setFillHeight(true);
                line.prefWidthProperty().bind(pane.widthProperty());
                line.setSpacing(8);

                var name = new Label();
                name.textProperty().bind(entry.name());
                name.prefHeightProperty().bind(line.heightProperty());
                name.setMinWidth(Region.USE_PREF_SIZE);
                name.setAlignment(Pos.CENTER_LEFT);
                if (compRegion != null) {
                    name.visibleProperty().bind(compRegion.visibleProperty());
                    name.managedProperty().bind(compRegion.managedProperty());
                }
                nameRegions.add(name);
                line.getChildren().add(name);

                if (compRegion != null) {
                    compRegion.accessibleTextProperty().bind(name.textProperty());
                    line.getChildren().add(compRegion);
                    HBox.setHgrow(compRegion, Priority.ALWAYS);
                }

                pane.getChildren().add(line);
            } else {
                if (compRegion != null) {
                    pane.getChildren().add(compRegion);
                }
            }

            var last = entry.equals(entries.getLast());
            if (!last) {
                Spacer spacer = new Spacer(7, Orientation.VERTICAL);
                pane.getChildren().add(spacer);
                if (compRegion != null) {
                    spacer.visibleProperty().bind(compRegion.visibleProperty());
                    spacer.managedProperty().bind(compRegion.managedProperty());
                }
            }
        }

        if (entries.size() == 1 && firstComp != null) {
            firstComp.visibleProperty().subscribe(v -> {
                pane.setVisible(v);
            });
            firstComp.managedProperty().subscribe(v -> {
                pane.setManaged(v);
            });
        }

        for (Region nameRegion : nameRegions) {
            nameRegion.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }

        if (entries.stream().anyMatch(entry -> entry.name() != null && entry.description() == null)) {
            var nameWidthBinding = Bindings.createDoubleBinding(
                    () -> {
                        return nameRegions.stream()
                                .map(Region::getWidth)
                                .filter(aDouble -> aDouble > 0.0)
                                .max(Double::compareTo)
                                .orElse(Region.USE_COMPUTED_SIZE);
                    },
                    nameRegions.stream().map(Region::widthProperty).toList().toArray(new Observable[0]));
            BindingsHelper.preserve(pane, nameWidthBinding);
            nameWidthBinding.addListener((observableValue, number, t1) -> {
                Platform.runLater(() -> {
                    for (Region nameRegion : nameRegions) {
                        nameRegion.setPrefWidth(t1.doubleValue());
                    }
                });
            });
        }

        Region finalFirstComp = firstComp;
        pane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                return;
            }

            var checks = validator.getActiveChecks();
            var failed = checks.stream()
                    .filter(check -> check.getValidationResult().getMessages().size() > 0)
                    .findFirst();
            if (failed.isPresent()) {
                var targets = failed.get().getTargets();
                if (targets.size() > 0) {
                    var r = targets.getFirst();
                    r.requestFocus();
                }
            } else {
                if (finalFirstComp != null) {
                    finalFirstComp.requestFocus();
                }
            }
        });

        return new SimpleCompStructure<>(pane);
    }

    public record Entry(
            String key,
            ObservableValue<String> description,
            String documentationLink,
            ObservableValue<String> name,
            Comp<?> comp) {}
}
