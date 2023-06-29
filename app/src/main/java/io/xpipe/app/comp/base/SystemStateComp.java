package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class SystemStateComp extends SimpleComp {


    public SystemStateComp(ObservableValue<String> name, ObservableValue<State> state) {
        this.name = name;
        this.state = state;
    }

    public static enum State {
        STOPPED,
        RUNNING,
        OTHER
    }

    private final ObservableValue<String> name;
    private final ObservableValue<State> state;

    @Override
    protected Region createSimple() {
        var icon = PlatformThread.sync(Bindings.createStringBinding(() -> {
            return state.getValue() == State.STOPPED ? "mdmz-stop_circle" : state.getValue() == State.RUNNING ? "mdrmz-play_circle_outline" : "mdmz-remove_circle_outline";
        }, state));
        var fi = new FontIcon();
        SimpleChangeListener.apply(icon, val -> fi.setIconLiteral(val));
        new FancyTooltipAugment<>(PlatformThread.sync(name)).augment(fi);

        var pane = new Pane(fi);
        return pane;
    }
}
