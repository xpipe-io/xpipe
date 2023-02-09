package io.xpipe.app.comp.source;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.extension.DataSourceTarget;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.impl.DynamicOptionsComp;
import io.xpipe.extension.fxcomps.impl.HorizontalComp;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import io.xpipe.extension.util.BusyProperty;
import io.xpipe.extension.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public class DsDataTransferComp extends SimpleComp {

    private final Property<DataSourceEntry> dataSourceEntry;
    Property<DataSourceTarget> selectedTarget = new SimpleObjectProperty<>();
    Property<DataSourceTarget.InstructionsDisplay> selectedDisplay = new SimpleObjectProperty<>();
    List<DataSourceTarget> excludedTargets = new ArrayList<>();

    public DsDataTransferComp selectApplication(DataSourceTarget t) {
        selectedTarget.setValue(t);
        return this;
    }

    public DsDataTransferComp exclude(DataSourceTarget t) {
        excludedTargets.add(t);
        return this;
    }

    public static void showPipeWindow(DataSourceEntry e) {
        Platform.runLater(() -> {
            var loading = new SimpleBooleanProperty();
            AppWindowHelper.sideWindow(
                            I18n.get("pipeDataSource"),
                            window -> {
                                var ms = new DsDataTransferComp(new SimpleObjectProperty<>(e))
                                        .exclude(DataSourceTarget.byId("base.saveSource")
                                                .orElseThrow());
                                var multi = new MultiStepComp() {
                                    @Override
                                    protected List<Entry> setup() {
                                        return List.of(new Entry(null, new Step<>(null) {
                                            @Override
                                            public CompStructure<?> createBase() {
                                                return ms.createStructure();
                                            }

                                            @Override
                                            public boolean canContinue() {
                                                var selected = ms.selectedTarget.getValue();
                                                if (selected == null) {
                                                    return false;
                                                }

                                                var validator = ms.selectedDisplay
                                                        .getValue()
                                                        .getValidator();
                                                if (validator == null) {
                                                    return true;
                                                }

                                                return validator.validate();
                                            }
                                        }));
                                    }

                                    @Override
                                    protected void finish() {
                                        var onFinish = ms.getSelectedDisplay()
                                                .getValue()
                                                .getOnFinish();
                                        if (onFinish != null) {
                                            ThreadHelper.runAsync(() -> {
                                                try (var busy = new BusyProperty(loading)) {
                                                    onFinish.run();
                                                    PlatformThread.runLaterIfNeeded(() -> window.close());
                                                }
                                            });
                                        }
                                    }
                                };
                                return multi.apply(s -> {
                                    SimpleChangeListener.apply(ms.getSelectedTarget(), (c) -> {
                                        if (c != null && c.getAccessType() == DataSourceTarget.AccessType.PASSIVE) {
                                            ((Region) s.get().getChildren().get(2)).setMaxHeight(0);
                                            ((Region) s.get().getChildren().get(2)).setMinHeight(0);
                                            ((Region) s.get().getChildren().get(2)).setVisible(false);
                                        } else {

                                            ((Region) s.get().getChildren().get(2)).setMaxHeight(Region.USE_PREF_SIZE);
                                            ((Region) s.get().getChildren().get(2)).setMinHeight(Region.USE_PREF_SIZE);
                                            ((Region) s.get().getChildren().get(2)).setVisible(true);
                                        }
                                    });
                                    s.get().setPrefWidth(600);
                                    s.get().setPrefHeight(700);
                                    AppFont.medium(s.get());
                                });
                            },
                            false,
                            loading)
                    .show();
        });
    }

    @Override
    public Region createSimple() {
        ObservableValue<DataSourceId> id = Bindings.createObjectBinding(
                () -> {
                    if (!DataStorage.get().getSourceEntries().contains(dataSourceEntry.getValue())) {
                        return null;
                    }

                    return DataStorage.get().getId(dataSourceEntry.getValue());
                },
                dataSourceEntry);

        var chooser = DataSourceTargetChoiceComp.create(
                selectedTarget,
                a -> !excludedTargets.contains(a)
                        && a.isApplicable(dataSourceEntry.getValue().getSource())
                        && a.createRetrievalInstructions(
                                        dataSourceEntry.getValue().getSource(), id)
                                != null);

        var setupGuideButton = new ButtonComp(
                        I18n.observable("setupGuide"), new FontIcon("mdoal-integration_instructions"), () -> {
                            Hyperlinks.open(selectedTarget.getValue().getSetupGuideURL());
                        })
                .apply(s -> s.get()
                        .visibleProperty()
                        .bind(Bindings.createBooleanBinding(
                                () -> {
                                    return selectedTarget.getValue() != null
                                            && selectedTarget.getValue().getSetupGuideURL() != null;
                                },
                                selectedTarget)));
        var top = new HorizontalComp(List.<Comp<?>>of(
                        chooser.apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS)), setupGuideButton))
                .apply(struc -> {
                    struc.get().setAlignment(Pos.CENTER);
                    struc.get().setSpacing(12);
                    struc.get().getStyleClass().add("top");
                });

        // setupGuideButton.prefHeightProperty().bind(chooserR.heightProperty());

        var content = new VBox(
                new DynamicOptionsComp(List.of(new DynamicOptionsComp.Entry(null, null, top)), false).createRegion(),
                new Region());
        SimpleChangeListener.apply(selectedTarget, c -> {
            if (selectedTarget.getValue() == null) {
                content.getChildren().set(1, new Region());
                selectedDisplay.setValue(null);
                return;
            }

            var instructions = selectedTarget
                    .getValue()
                    .createRetrievalInstructions(dataSourceEntry.getValue().getSource(), id);
            content.getChildren().set(1, instructions.getRegion());
            VBox.setVgrow(instructions.getRegion(), Priority.ALWAYS);
            selectedDisplay.setValue(instructions);
        });

        content.setSpacing(15);
        var r = content;
        r.getStyleClass().add("data-source-retrieve");
        return r;
    }
}
