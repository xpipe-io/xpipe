package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.Element;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleControl;
import com.dlsc.preferencesfx.formsfx.view.renderer.PreferencesFxFormRenderer;
import com.dlsc.preferencesfx.formsfx.view.renderer.PreferencesFxGroup;
import com.dlsc.preferencesfx.formsfx.view.renderer.PreferencesFxGroupRenderer;
import com.dlsc.preferencesfx.util.PreferencesFxUtils;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.stream.Collectors;

public class CustomFormRenderer extends PreferencesFxFormRenderer {

    public static final double SPACING = 8.0;

    public CustomFormRenderer(Form form) {
        super(form);
    }

    @Override
    public void initializeParts() {
        groups = form.getGroups().stream()
                .map(group -> new PreferencesFxGroupRenderer((PreferencesFxGroup) group, this) {

                    @Override
                    public void initializeParts() {
                        super.initializeParts();
                        grid.getStyleClass().add("grid");
                    }

                    @Override
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    public void layoutParts() {
                        StringBuilder styleClass = new StringBuilder("group");

                        // if there are no rows yet, getRowCount returns -1, in this case the next row is 0
                        int nextRow = PreferencesFxUtils.getRowCount(grid) + 1;

                        List<Element> elements = preferencesGroup.getElements().stream()
                                .map(Element.class::cast)
                                .toList();

                        // Only when the preferencesGroup has a title
                        if (preferencesGroup.getTitle() != null && elements.size() > 0) {
                            titleLabel.setPrefWidth(USE_COMPUTED_SIZE);
                            grid.add(titleLabel, 0, nextRow++);
                            titleLabel.getStyleClass().add("group-title");
                            AppFont.setSize(titleLabel, 2);
                            // Set margin for all but first group titles to visually separate groups
                            if (nextRow > 1) {
                                GridPane.setMargin(titleLabel, new Insets(SPACING * 5, 0, SPACING, 0));
                            } else {
                                GridPane.setMargin(titleLabel, new Insets(SPACING, 0, SPACING, 0));
                            }
                        }

                        styleClass.append("-setting");

                        int rowAmount = nextRow;
                        for (int i = 0; i < elements.size(); i++) {
                            // add to GridPane
                            Element element = elements.get(i);
                            if (element instanceof Field f) {
                                SimpleControl c = (SimpleControl) f.getRenderer();
                                c.setField(f);
                                AppFont.normal(c.getFieldLabel());
                                c.getFieldLabel().setPrefHeight(AppFont.getPixelSize(1));
                                c.getFieldLabel().setMaxHeight(AppFont.getPixelSize(1));
                                c.getFieldLabel().textProperty().unbind();
                                c.getFieldLabel().textProperty().bind(Bindings.createStringBinding(() -> {
                                    return f.labelProperty().get() + (AppPrefs.get().getProRequiredSettings().contains(f) ? " (Pro)" : "");
                                }, f.labelProperty()));
                                grid.add(c.getFieldLabel(), 0, i + rowAmount);

                                var canFocus = BindingsHelper.persist(
                                        c.getNode().disabledProperty().not());

                                var descriptionLabel = new Label();
                                AppFont.medium(descriptionLabel);
                                descriptionLabel.setWrapText(true);
                                descriptionLabel
                                        .disableProperty()
                                        .bind(c.getFieldLabel().disabledProperty());
                                descriptionLabel
                                        .opacityProperty()
                                        .bind(c.getFieldLabel()
                                                .opacityProperty()
                                                .multiply(0.65));
                                descriptionLabel
                                        .managedProperty()
                                        .bind(c.getFieldLabel().managedProperty());
                                descriptionLabel
                                        .visibleProperty()
                                        .bind(c.getFieldLabel().visibleProperty());

                                var descriptionKey = f.getLabel() != null ? f.getLabel() + "Description" : null;
                                if (AppI18n.getInstance().containsKey(descriptionKey)) {
                                    rowAmount++;
                                    descriptionLabel.textProperty().bind(AppI18n.observable(descriptionKey));
                                    descriptionLabel.focusTraversableProperty().bind(canFocus);
                                    grid.add(descriptionLabel, 0, i + rowAmount);
                                }

                                rowAmount++;

                                var node = c.getNode();
                                ((Region) node).setMaxWidth(250);
                                ((Region) node).setMinWidth(250);
                                AppFont.medium(c.getNode());
                                c.getFieldLabel().focusTraversableProperty().bind(canFocus);
                                grid.add(node, 0, i + rowAmount);

                                if (i == elements.size() - 1) {
                                    // additional styling for the last setting
                                    styleClass.append("-last");
                                }

                                var offset = preferencesGroup.getTitle() != null ? 15 : 0;

                                GridPane.setMargin(descriptionLabel, new Insets(SPACING, 0, 0, offset));
                                GridPane.setMargin(node, new Insets(SPACING, 0, 0, offset));

                                if (!((i == 0) && (nextRow > 0))) {
                                    GridPane.setMargin(c.getFieldLabel(), new Insets(SPACING * 6, 0, 0, offset));
                                } else {
                                    GridPane.setMargin(c.getFieldLabel(), new Insets(SPACING, 0, 0, offset));
                                }

                                c.getFieldLabel().getStyleClass().add(styleClass + "-label");
                                node.getStyleClass().add(styleClass + "-node");
                            }

                            if (element instanceof LazyNodeElement<?> nodeElement) {
                                var node = nodeElement.getNode();
                                grid.add(node, 0, i + rowAmount);
                            }
                        }
                    }
                })
                .collect(Collectors.toList());
    }
}
