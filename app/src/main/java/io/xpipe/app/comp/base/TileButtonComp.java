package io.xpipe.app.comp.base;

import atlantafx.base.theme.Styles;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

@AllArgsConstructor
@Getter
public class TileButtonComp extends Comp<TileButtonComp.Structure> {

    private final ObservableValue<String> name;
    private final ObservableValue<String> description;
    private final ObservableValue<String> icon;
    private final Consumer<ActionEvent> action;

    public TileButtonComp(String nameKey, String descriptionKey, String icon, Consumer<ActionEvent> action) {
        this.name = AppI18n.observable(nameKey);
        this.description = AppI18n.observable(descriptionKey);
        this.icon = new SimpleStringProperty(icon);
        this.action = action;
    }

    @Override
    public Structure createBase() {
        var bt = new Button();
        Styles.toggleStyleClass(bt, Styles.FLAT);
        bt.setOnAction(e -> {
            action.accept(e);
        });

        var header = new Label();
        header.textProperty().bind(PlatformThread.sync(name));
        var desc = new Label();
        desc.textProperty().bind(PlatformThread.sync(description));
        AppFont.small(desc);
        desc.setOpacity(0.65);
        var text = new VBox(header, desc);
        text.setSpacing(2);

        var fi = new FontIcon();
        SimpleChangeListener.apply(PlatformThread.sync(icon), val -> {
            fi.setIconLiteral(val);
        });

        var pane = new StackPane(fi);
        var hbox = new HBox(pane, text);
        hbox.setSpacing(8);
        pane.prefWidthProperty().bind(
                Bindings.createDoubleBinding(() -> (header.getHeight() + desc.getHeight()) * 0.6, header.heightProperty(), desc.heightProperty()));
        pane.prefHeightProperty().bind(
                Bindings.createDoubleBinding(() -> header.getHeight() + desc.getHeight() + 2, header.heightProperty(), desc.heightProperty()));
        pane.prefHeightProperty().addListener((c, o, n) -> {
            var size = Math.min(n.intValue(), 100);
            fi.setIconSize((int) (size * 0.55));
        });
        bt.setGraphic(hbox);
        return Structure.builder().graphic(fi).button(bt).content(hbox).name(header).description(desc).build();
    }

    @Value
    @Builder
    public static class Structure implements CompStructure<Button> {
        Button button;
        HBox content;
        FontIcon graphic;
        Label name;
        Label description;

        @Override
        public Button get() {
            return button;
        }
    }
}
