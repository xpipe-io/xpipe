package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.*;

import java.net.URI;

public class XPipeUrlActionProvider implements LauncherActionProvider {

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
