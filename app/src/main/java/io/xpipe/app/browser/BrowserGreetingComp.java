package io.xpipe.app.browser;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.time.LocalDateTime;

public class BrowserGreetingComp extends SimpleComp {

    @Override
    protected Region createSimple() {
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
        var r = new Label(text);
        AppFont.setSize(r, 7);
        return r;
    }
}
