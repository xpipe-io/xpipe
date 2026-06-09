package io.xpipe.app.comp.base;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.FailableSupplier;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.concurrent.atomic.AtomicReference;

@Getter
@AllArgsConstructor
public class BusyButtonComp extends RegionBuilder<Button> {

    private final ObservableValue<String> name;
    private final FailableSupplier<Boolean> run;

    @Override
    public Button createSimple() {
        var busy = new SimpleBooleanProperty();
        AtomicReference<Region> button = new AtomicReference<>();
        var testButton = new ButtonComp(name, new FontIcon("mdi2p-play"), () -> {
            ThreadHelper.runAsync(() -> {
                try {
                    BooleanScope.executeExclusive(busy, () -> {
                        run.get();
                    });
                } catch (Throwable e) {
                    Platform.runLater(() -> {
                        button.get().getStyleClass().add(Styles.DANGER);
                    });
                    ErrorEventFactory.fromThrowable(e).expected().handle();
                }
            });
        });
        testButton.apply(struc -> {
            struc.disableProperty().bind(PlatformThread.sync(busy));
            struc.graphicProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> {
                                return busy.get()
                                        ? new LoadingIconComp(busy, AppFontSizes::base)
                                          .style("busy-loading-icon")
                                          .build()
                                        : null;
                            },
                            PlatformThread.sync(busy)));
        });
        testButton.apply(struc -> button.set(struc));
        testButton.padding(new Insets(6, 10, 6, 6));
        return testButton.build();
    }
}
