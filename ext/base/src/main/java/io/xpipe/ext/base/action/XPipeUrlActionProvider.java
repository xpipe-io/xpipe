package io.xpipe.ext.base.action;

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
        var actions = ActionUrls.parse(query);
        return actions.size() == 1 ? actions.getFirst() : null;
    }
}
