package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;

public class Hyperlinks {

    public static final String DOUBLE_PROMPT = "https://docs.xpipe.io/troubleshoot/two-step-connections";
    public static final String AGENT_SETUP = "https://docs.xpipe.io/troubleshoot/ssh-agent-socket";
    public static final String GITHUB = "https://github.com/xpipe-io/xpipe";
    public static final String GITHUB_PTB = "https://github.com/xpipe-io/xpipe-ptb";
    public static final String GITHUB_LATEST = "https://github.com/xpipe-io/xpipe/releases/latest";
    public static final String GITHUB_PYTHON_API = "https://github.com/xpipe-io/xpipe-python-api";
    public static final String PRIVACY = "https://docs.xpipe.io/legal/privacy-policy";
    public static final String EULA = "https://docs.xpipe.io/legal/end-user-license-agreement";
    public static final String SECURITY = "https://docs.xpipe.io/reference/security";
    public static final String TRANSLATE = "https://github.com/xpipe-io/xpipe/tree/master/lang";
    public static final String DISCORD = "https://discord.gg/8y89vS8cRb";
    public static final String GITHUB_WEBTOP = "https://github.com/xpipe-io/xpipe-webtop";
    public static final String SELFHST_ICONS = "https://github.com/selfhst/icons";
    public static final String SLACK =
            "https://join.slack.com/t/XPipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg";

    static final String[] browsers = {
        "xdg-open", "google-chrome", "firefox", "opera", "konqueror", "mozilla", "gnome-open", "open"
    };

    @SuppressWarnings("deprecation")
    public static void open(String uri) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Runtime.getRuntime().exec("open " + uri);
            } else if (osName.startsWith("Windows")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + uri);
            } else { // assume Unix or Linux
                String browser = null;
                for (String b : browsers) {
                    if (browser == null
                            && Runtime.getRuntime()
                                            .exec(new String[] {"which", b})
                                            .getInputStream()
                                            .read()
                                    != -1) {
                        Runtime.getRuntime().exec(new String[] {browser = b, uri});
                    }
                }
                if (browser == null) {
                    throw new Exception("No web browser or URL opener found to open " + uri);
                }
            }
        } catch (Exception e) {
            // should not happen
            // dump stack for debug purpose
            ErrorEvent.fromThrowable(e).handle();
        }
    }
}
