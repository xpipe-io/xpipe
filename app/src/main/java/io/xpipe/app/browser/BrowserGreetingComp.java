package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.time.LocalDateTime;

public class BrowserGreetingComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var r = new Label(getText());
        AppLayoutModel.get().getSelected().addListener((observableValue, entry, t1) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                r.setText(getText());
            });
        });
        AppFont.setSize(r, 7);
        r.getStyleClass().add(Styles.TEXT_BOLD);
        return r;
    }

    private String getText() {
        var ldt = LocalDateTime.now();
        var hour = ldt.getHour();
        String text;
        if (hour > 18 || hour < 5) {
            text = "Good evening";
        } else if (hour < 12) {
            text = "Good morning";
        } else {
            text = "Good afternoon";
        }
        return text;
    }
}
