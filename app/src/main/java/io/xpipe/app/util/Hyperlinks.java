package io.xpipe.app.util;

import io.xpipe.app.core.App;
import io.xpipe.extension.event.ErrorEvent;

import java.awt.*;
import java.net.URI;

public class Hyperlinks {

    public static final String WEBSITE = "https://xpipe.io";
    public static final String DOCUMENTATION = "https://xpipe.io/docs";
    public static final String GITHUB = "https://github.com/xpipe-io";
    public static final String DISCORD = "https://discord.gg/8y89vS8cRb";
    public static final String SLACK =
            "https://join.slack.com/t/x-pipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg";
    public static final String DOCS_PRIVACY = "https://xpipe.io/docs/privacy";

    public static Runnable openLink(String s) {
        return () -> open(s);
    }

    public static void open(String url) {
        var t = new Thread(() -> {
            try {
                if (App.getApp() != null) {
                    App.getApp().getHostServices().showDocument(url);
                } else {
                    Desktop.getDesktop().browse(URI.create(url));
                }
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).build().handle();
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
