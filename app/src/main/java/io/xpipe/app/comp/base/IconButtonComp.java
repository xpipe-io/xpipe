package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.control.Button;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

public class IconButtonComp extends Comp<CompStructure<Button>> {

    private final ObservableValue<? extends LabelGraphic> icon;
    private final Runnable listener;

    public IconButtonComp(String defaultVal) {
        this(new SimpleObjectProperty<>(new LabelGraphic.IconGraphic(defaultVal)), null);
    }

    public IconButtonComp(String defaultVal, Runnable listener) {
        this(new SimpleObjectProperty<>(new LabelGraphic.IconGraphic(defaultVal)), listener);
    }

    public IconButtonComp(ObservableValue<? extends LabelGraphic> icon) {
        this.icon = icon;
        this.listener = null;
    }

    public IconButtonComp(LabelGraphic defaultVal, Runnable listener) {
        this(new SimpleObjectProperty<>(defaultVal), listener);
    }

    public IconButtonComp(ObservableValue<? extends LabelGraphic> icon, Runnable listener) {
        this.icon = icon;
        this.listener = listener;
    }

    @Override
    public CompStructure<Button> createBase() {
        var button = new Button();
        button.getStyleClass().add(Styles.FLAT);
        // AtlantaFX sets underline to true. This bugs out ikonli: https://github.com/kordamp/ikonli/issues/175
        button.setUnderline(false);
        icon.subscribe(labelGraphic -> {
            PlatformThread.runLaterIfNeeded(() -> {
                button.setGraphic(labelGraphic.createGraphicNode());
                if (button.getGraphic() instanceof FontIcon fi) {
                    fi.setIconSize((int) new Size(button.getFont().getSize(), SizeUnits.PT).pixels());
                }
            });
        });
        button.fontProperty().subscribe((n) -> {
            if (button.getGraphic() instanceof FontIcon fi) {
                fi.setIconSize((int) new Size(n.getSize(), SizeUnits.PT).pixels());
            }
        });
        if (listener != null) {
            button.setOnAction(e -> {
                listener.run();
                e.consume();
            });
        }
        button.getStyleClass().add("icon-button-comp");
        return new SimpleCompStructure<>(button);
    }
}
