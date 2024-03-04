package io.xpipe.app.comp.base;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.core.process.ShellStoreState;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.javafx.StackedFontIcon;

@Getter
public class SystemStateComp extends SimpleComp {

    private final ObservableValue<State> state;

    public SystemStateComp(ObservableValue<State> state) {
        this.state = state;
    }

    @Override
    protected Region createSimple() {
        var icon = PlatformThread.sync(Bindings.createStringBinding(
                () -> {
                    return state.getValue() == State.FAILURE
                            ? "mdi2l-lightning-bolt"
                            : state.getValue() == State.SUCCESS ? "mdal-check" : "mdsmz-remove";
                },
                state));
        var fi = new FontIcon();
        fi.getStyleClass().add("inner-icon");
        SimpleChangeListener.apply(icon, val -> fi.setIconLiteral(val));

        var border = new FontIcon("mdi2c-circle-outline");
        border.getStyleClass().add("outer-icon");
        border.setOpacity(0.5);

        var success = Styles.toDataURI(
                ".stacked-ikonli-font-icon > .outer-icon { -fx-icon-color: -color-success-emphasis; }");
        var failure =
                Styles.toDataURI(".stacked-ikonli-font-icon > .outer-icon { -fx-icon-color: -color-danger-emphasis; }");
        var other =
                Styles.toDataURI(".stacked-ikonli-font-icon > .outer-icon { -fx-icon-color: -color-accent-emphasis; }");

        var pane = new StackedFontIcon();
        pane.getChildren().addAll(fi, border);
        pane.setAlignment(Pos.CENTER);

        var dataClass1 =
                """
            .stacked-ikonli-font-icon > .outer-icon {
                -fx-icon-size: 22px;
            }
            .stacked-ikonli-font-icon > .inner-icon {
                -fx-icon-size: 12px;
            }
            """;
        pane.getStylesheets().add(Styles.toDataURI(dataClass1));

        SimpleChangeListener.apply(PlatformThread.sync(state), val -> {
            pane.getStylesheets().removeAll(success, failure, other);
            pane.getStylesheets().add(val == State.SUCCESS ? success : val == State.FAILURE ? failure : other);
        });

        return pane;
    }

    public enum State {
        FAILURE,
        SUCCESS,
        OTHER;

        public static ObservableValue<State> shellState(StoreEntryWrapper w) {
            return BindingsHelper.map(w.getPersistentState(), o -> {
                if (o instanceof ShellStoreState s) {
                    if (s.getShellDialect() != null
                            && !s.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
                        return SUCCESS;
                    }

                    return s.getRunning() != null ? s.getRunning() ? SUCCESS : FAILURE : OTHER;
                }

                return OTHER;
            });
        }
    }
}
