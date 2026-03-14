package io.xpipe.app.action;

import java.net.URI;

public class XPipeUrlProvider implements LauncherUrlProvider {

    @Override
    public String getScheme() {
        return "xpipe";
    }

    @Override
    public AbstractAction createAction(URI uri) throws Exception {
        var a = uri.getHost();
        if (!"action".equals(a)) {
            return null;
        }

        var query = uri.getQuery();
        var action = ActionUrls.parse(query);
        return action.orElse(null);
    }
}
