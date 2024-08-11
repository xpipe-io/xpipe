package io.xpipe.app.fxcomps.impl;

import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.LabelGraphic;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;

public class IconButtonComp extends Comp<CompStructure<Button>> {

    private final ObservableValue<? extends LabelGraphic> icon;
    private final Runnable listener;

    public IconButtonComp(String defaultVal) {
        this(new SimpleObjectProperty<>(new LabelGraphic.IconGraphic(defaultVal)), null);
    }

    public IconButtonComp(String defaultVal, Runnable listener) {
        this(new SimpleObjectProperty<>(new LabelGraphic.IconGraphic(defaultVal)), listener);
    }

    public IconButtonComp(LabelGraphic defaultVal) {
        this(new SimpleObjectProperty<>(defaultVal), null);
    }

    public IconButtonComp(ObservableValue<? extends LabelGraphic> icon) {
        this.icon = icon;
        this.listener = null;
    }

    public IconButtonComp(LabelGraphic defaultVal, Runnable listener) {
        this(new SimpleObjectProperty<>(defaultVal), listener);
    }

    public IconButtonComp(ObservableValue<? extends LabelGraphic> icon, Runnable listener) {
        this.icon = PlatformThread.sync(icon);
        this.listener = listener;
    }

    @Override
    public CompStructure<Button> createBase() {
        var button = new Button();
        button.getStyleClass().add(Styles.FLAT);

//        var fi = new FontIcon(icon.getValue());
//        fi.setFocusTraversable(false);
//        icon.addListener((c, o, n) -> {
//            fi.setIconLiteral(n);
//        });
//        fi.setIconSize((int) new Size(fi.getFont().getSize(), SizeUnits.PT).pixels());
//        button.fontProperty().addListener((c, o, n) -> {
//            fi.setIconSize((int) new Size(n.getSize(), SizeUnits.PT).pixels());
//        });
//        // fi.iconColorProperty().bind(button.textFillProperty());
//        button.setGraphic(fi);
        icon.subscribe(labelGraphic -> {
            button.setGraphic(labelGraphic.createGraphicNode());
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
