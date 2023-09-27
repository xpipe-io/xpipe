package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import lombok.Builder;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

public class FilterComp extends Comp<FilterComp.Structure> {

    private final Property<String> filterText;

    public FilterComp(Property<String> filterText) {
        this.filterText = filterText;
    }

    @Override
    public Structure createBase() {
        var fi = new FontIcon("mdi2m-magnify");
        var bgLabel = new Label("Search ...", fi);
        bgLabel.getStyleClass().add("filter-background");
        var filter = new TextField();
        filter.setAccessibleText("Filter");

        SimpleChangeListener.apply(filterText, val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (!Objects.equals(filter.getText(), val)) {
                    filter.setText(val);
                }
            });
        });
        filter.textProperty().addListener((observable, oldValue, newValue) -> {
            // Handle pasted xpipe URLs
            if (newValue != null && newValue.startsWith("xpipe://")) {
                AppActionLinkDetector.handle(newValue, false);
                filter.setText(null);
                return;
            }

            filterText.setValue(newValue);
        });

        bgLabel.visibleProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> (filter.getText() == null || filter.getText().isEmpty()),
                        filter.textProperty(),
                        filter.focusedProperty()));

        var stack = new StackPane(bgLabel, filter);
        stack.getStyleClass().add("filter-comp");

        return Structure.builder()
                .inactiveIcon(fi)
                .inactiveText(bgLabel)
                .text(filter)
                .pane(stack)
                .build();
    }

    @Value
    @Builder
    public static class Structure implements CompStructure<StackPane> {
        StackPane pane;
        Node inactiveIcon;
        Label inactiveText;
        TextField text;

        @Override
        public StackPane get() {
            return pane;
        }
    }
}
