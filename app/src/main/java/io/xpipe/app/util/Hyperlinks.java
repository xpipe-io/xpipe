package io.xpipe.app.util;

public class Hyperlinks {

    public static final String GITHUB = "https://github.com/xpipe-io/xpipe";
    public static final String GITHUB_PTB = "https://github.com/xpipe-io/xpipe-ptb";
    public static final String GITHUB_LATEST = "https://github.com/xpipe-io/xpipe/releases/latest";
    public static final String TRANSLATE = "https://github.com/xpipe-io/xpipe/tree/master/lang";
    public static final String DISCORD = "https://discord.gg/8y89vS8cRb";
    public static final String REDDIT = "https://reddit.com/r/xpipe";
    public static final String GITHUB_WEBTOP = "https://github.com/xpipe-io/xpipe-webtop";

    public static void open(String uri) {
        DesktopHelper.openUrl(uri);
    }
}
