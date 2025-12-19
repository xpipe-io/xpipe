package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompDescriptor;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Spacer;
import lombok.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

@Getter
public class TileButtonComp extends Comp<TileButtonComp.Structure> {

    private final ObservableValue<String> name;
    private final ObservableValue<String> description;
    private final ObservableValue<String> icon;
    private final Consumer<ActionEvent> action;

    @Setter
    private double iconSize = 0.55;

    @Setter
    private Comp<?> right;

    public TileButtonComp(String nameKey, String descriptionKey, String icon, Consumer<ActionEvent> action) {
        this.name = AppI18n.observable(nameKey);
        this.description = AppI18n.observable(descriptionKey);
        this.icon = new SimpleStringProperty(icon);
        this.action = action;
    }

    public TileButtonComp(
            ObservableValue<String> name,
            ObservableValue<String> description,
            ObservableValue<String> icon,
            Consumer<ActionEvent> action) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.action = action;
    }

    @Override
    public Structure createBase() {
        var bt = new Button();
        CompDescriptor.builder().name(name).description(description).build().apply(bt);
        bt.getStyleClass().add("tile-button-comp");
        bt.setOnAction(e -> {
            if (action != null) {
                action.accept(e);
            }
        });

        var header = new Label();
        name.subscribe(value -> {
            PlatformThread.runLaterIfNeeded(() -> {
                header.setText(value);
            });
        });
        var desc = new Label();
        description.subscribe(value -> {
            PlatformThread.runLaterIfNeeded(() -> {
                desc.setText(value);
            });
        });
        AppFontSizes.xs(desc);
        desc.setOpacity(0.8);
        var text = new VBox(header, desc);
        text.setSpacing(2);

        var fi = new FontIconComp(icon).createStructure();
        var pane = fi.getPane();
        var hbox = new HBox(pane, text);
        Region rightRegion = right != null ? right.createRegion() : null;
        if (rightRegion != null) {
            hbox.getChildren().add(new Spacer());
            hbox.getChildren().add(rightRegion);
        }
        hbox.setSpacing(8);
        pane.prefWidthProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> (header.getHeight() + desc.getHeight()) * 0.6,
                        header.heightProperty(),
                        desc.heightProperty()));
        pane.prefHeightProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> header.getHeight() + desc.getHeight() + 2,
                        header.heightProperty(),
                        desc.heightProperty()));
        pane.prefHeightProperty().addListener((c, o, n) -> {
            var size = Math.min(n.intValue(), 100);
            fi.getIcon().setIconSize((int) (size * iconSize));
        });
        bt.setGraphic(hbox);
        return Structure.builder()
                .graphic(fi.getIcon())
                .button(bt)
                .content(hbox)
                .name(header)
                .description(desc)
                .right(rightRegion)
                .build();
    }

    @Value
    @Builder
    public static class Structure implements CompStructure<Button> {
        Button button;
        HBox content;
        FontIcon graphic;
        Label name;
        Label description;
        Region right;

        @Override
        public Button get() {
            return button;
        }
    }
}
