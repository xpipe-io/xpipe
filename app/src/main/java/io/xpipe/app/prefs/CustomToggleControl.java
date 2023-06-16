package io.xpipe.app.prefs;

import atlantafx.base.controls.ToggleSwitch;
import com.dlsc.formsfx.model.structure.BooleanField;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleControl;
import com.dlsc.preferencesfx.formsfx.view.controls.ToggleControl;
import com.dlsc.preferencesfx.util.VisibilityProperty;
import javafx.scene.control.Label;

/**
 * Displays a control for boolean values with a toggle from ControlsFX.
 *
 * @author François Martin
 * @author Marco Sanfratello
 */
public class CustomToggleControl extends SimpleControl<BooleanField, ToggleSwitch> {

    /**
     * Constructs a ToggleControl of {@link ToggleControl} type, with visibility condition.
     *
     * @param visibilityProperty property for control visibility of this element
     *
     * @return the constructed ToggleControl
     */
    public static ToggleControl of(VisibilityProperty visibilityProperty) {
        ToggleControl toggleControl = new ToggleControl();

        toggleControl.setVisibilityProperty(visibilityProperty);

        return toggleControl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParts() {
        super.initializeParts();
        fieldLabel = new Label(field.labelProperty().getValue());
        node = new atlantafx.base.controls.ToggleSwitch();
        node.getStyleClass().add("toggle-control");
        node.setSelected(field.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void layoutParts() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupBindings() {
        super.setupBindings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupValueChangedListeners() {
        super.setupValueChangedListeners();
        field.userInputProperty().addListener((observable, oldValue, newValue) -> {
            node.setSelected(Boolean.parseBoolean(field.getUserInput()));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupEventHandlers() {
        node.selectedProperty().addListener((observable, oldValue, newValue) -> {
            field.userInputProperty().setValue(String.valueOf(newValue));
        });
    }
}
