package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;

import java.io.File;

public class Hyperlinks {

    public static final String DOCS = "https://docs.xpipe.io";
    public static final String GITHUB = "https://github.com/xpipe-io/xpipe";
    public static final String GITHUB_PTB = "https://github.com/xpipe-io/xpipe-ptb";
    public static final String GITHUB_LATEST = "https://github.com/xpipe-io/xpipe/releases/latest";
    public static final String GITHUB_PYTHON_API = "https://github.com/xpipe-io/xpipe-python-api";
    public static final String TRANSLATE = "https://github.com/xpipe-io/xpipe/tree/master/lang";
    public static final String DISCORD = "https://discord.gg/8y89vS8cRb";
    public static final String GITHUB_WEBTOP = "https://github.com/xpipe-io/xpipe-webtop";
    public static final String SLACK =
            "https://join.slack.com/t/XPipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg";

    public static void open(String uri) {
        DesktopHelper.openUrl(uri);
    }
}
