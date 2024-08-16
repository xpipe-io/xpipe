package io.xpipe.app.browser;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.core.process.OsType;

import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;

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
        if (OsType.getLocal() != OsType.MACOS) {
            r.getStyleClass().add(Styles.TEXT_BOLD);
        }
        return r;
    }

    private String getText() {
        var ldt = LocalDateTime.now();
        var hour = ldt.getHour();
        String text;
        if (hour > 18 || hour < 5) {
            text = AppI18n.get("goodEvening");
        } else if (hour < 12) {
            text = AppI18n.get("goodMorning");
        } else {
            text = AppI18n.get("goodAfternoon");
        }
        return text;
    }
}
