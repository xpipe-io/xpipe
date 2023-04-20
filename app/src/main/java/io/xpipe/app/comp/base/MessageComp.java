package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MessageComp extends SimpleComp {

    Property<Boolean> shown = new SimpleBooleanProperty();

    ObservableValue<String> text;
    int msShown;

    public MessageComp(ObservableValue<String> text, int msShown) {
        this.text = PlatformThread.sync(text);
        this.msShown = msShown;
    }

    public void show() {
        shown.setValue(true);

        if (msShown != -1) {
            ThreadHelper.runAsync(() -> {
                try {
                    Thread.sleep(msShown);
                } catch (InterruptedException ignored) {
                }

                shown.setValue(false);
            });
        }
    }

    @Override
    protected Region createSimple() {
        var l = new TextArea();
        l.textProperty().bind(text);
        l.setWrapText(true);
        l.getStyleClass().add("message");
        l.setEditable(false);

        var sp = new StackPane(l);
        sp.getStyleClass().add("message-comp");

        SimpleChangeListener.apply(PlatformThread.sync(shown), n -> {
            if (n) {
                l.setMinHeight(Region.USE_PREF_SIZE);
                l.setPrefHeight(Region.USE_COMPUTED_SIZE);
                l.setMaxHeight(Region.USE_PREF_SIZE);

                sp.setMinHeight(Region.USE_PREF_SIZE);
                sp.setPrefHeight(Region.USE_COMPUTED_SIZE);
                sp.setMaxHeight(Region.USE_PREF_SIZE);
            } else {
                l.setMinHeight(0);
                l.setPrefHeight(0);
                l.setMaxHeight(0);

                sp.setMinHeight(0);
                sp.setPrefHeight(0);
                sp.setMaxHeight(0);
            }
        });

        return sp;
    }
}
