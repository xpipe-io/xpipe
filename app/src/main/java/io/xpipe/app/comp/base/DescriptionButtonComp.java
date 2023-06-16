package io.xpipe.app.comp.base;

import atlantafx.base.theme.Styles;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

@AllArgsConstructor
@Getter
public class DescriptionButtonComp extends SimpleComp {

    private final ObservableValue<String> name;
    private final ObservableValue<String> description;
    private final ObservableValue<String> icon;
    private final Consumer<ActionEvent> action;

    public DescriptionButtonComp(String nameKey, String descriptionKey, String icon, Consumer<ActionEvent> action) {
        this.name = AppI18n.observable(nameKey);
        this.description = AppI18n.observable(descriptionKey);
        this.icon = new SimpleStringProperty(icon);
        this.action = action;
    }

    @Override
    protected Region createSimple() {
        var bt = new Button();
        bt.setGraphic(createNamedEntry());
        Styles.toggleStyleClass(bt, Styles.FLAT);
        bt.setOnAction(e -> {
            action.accept(e);
        });
        return bt;
    }

    private Region createNamedEntry() {
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
            fi.setIconSize((int) (size * 0.55));
        });
        return hbox;
    }
}
