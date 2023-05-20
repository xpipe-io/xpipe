package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.SingleSelectionField;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleControl;
import io.xpipe.app.util.Translatable;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class TranslatableComboBoxControl<V extends Translatable>
        extends SimpleControl<SingleSelectionField<V>, StackPane> {

    private ComboBox<V> comboBox;
    private Label readOnlyLabel;

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void initializeParts() {
        super.initializeParts();

        fieldLabel = new Label(field.labelProperty().getValue());
        readOnlyLabel = new Label();

        node = new StackPane();
        node.getStyleClass().add("simple-select-control");

        comboBox = new ComboBox<V>(field.getItems());
        comboBox.setConverter(Translatable.stringConverter());

        comboBox.getSelectionModel().select(field.getItems().indexOf(field.getSelection()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void layoutParts() {
        readOnlyLabel.getStyleClass().add("read-only-label");

        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setVisibleRowCount(10);

        node.setAlignment(Pos.CENTER_LEFT);
        node.getChildren().addAll(comboBox, readOnlyLabel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupBindings() {
        super.setupBindings();

        comboBox.visibleProperty().bind(field.editableProperty());
        readOnlyLabel.visibleProperty().bind(field.editableProperty().not());
        readOnlyLabel.textProperty().bind(Translatable.asTranslatedString(comboBox.valueProperty()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setupValueChangedListeners() {
        super.setupValueChangedListeners();

        field.itemsProperty().addListener((observable, oldValue, newValue) -> comboBox.setItems(field.getItems()));

        field.selectionProperty().addListener((observable, oldValue, newValue) -> {
            if (field.getSelection() != null) {
                comboBox.getSelectionModel().select(field.getItems().indexOf(field.getSelection()));
            } else {
                comboBox.getSelectionModel().clearSelection();
            }
        });

        field.errorMessagesProperty().addListener((observable, oldValue, newValue) -> toggleTooltip(comboBox));
        field.tooltipProperty().addListener((observable, oldValue, newValue) -> toggleTooltip(comboBox));
        comboBox.focusedProperty().addListener((observable, oldValue, newValue) -> toggleTooltip(comboBox));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupEventHandlers() {
        comboBox.valueProperty()
                .addListener((observable, oldValue, newValue) ->
                        field.select(comboBox.getSelectionModel().getSelectedIndex()));
    }
}
