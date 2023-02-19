package io.xpipe.app.fxcomps.impl;

import com.jfoenix.controls.JFXButton;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import org.kordamp.ikonli.javafx.FontIcon;

public class IconButtonComp extends Comp<CompStructure<JFXButton>> {

    private final ObservableValue<String> icon;
    private final Runnable listener;

    public IconButtonComp(String defaultVal) {
        this(new SimpleObjectProperty<>(defaultVal), null);
    }

    public IconButtonComp(String defaultVal, Runnable listener) {
        this(new SimpleObjectProperty<>(defaultVal), listener);
    }

    public IconButtonComp(ObservableValue<String> icon, Runnable listener) {
        this.icon = PlatformThread.sync(icon);
        this.listener = listener;
    }

    @Override
    public CompStructure<JFXButton> createBase() {
        var button = new JFXButton();

        var fi = new FontIcon(icon.getValue());
        icon.addListener((c, o, n) -> {
            fi.setIconLiteral(n);
        });
        fi.setIconSize((int) new Size(fi.getFont().getSize(), SizeUnits.PT).pixels());
        button.fontProperty().addListener((c, o, n) -> {
            fi.setIconSize((int) new Size(n.getSize(), SizeUnits.PT).pixels());
        });
        //fi.iconColorProperty().bind(button.textFillProperty());
        button.setGraphic(fi);
        button.setOnAction(e -> {
            e.consume();
            if (listener != null) {
                listener.run();
            }
        });
        button.getStyleClass().add("icon-button-comp");
        return new SimpleCompStructure<>(button);
    }
}
