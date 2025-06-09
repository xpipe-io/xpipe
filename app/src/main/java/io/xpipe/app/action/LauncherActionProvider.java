package io.xpipe.app.action;

import java.net.URI;

public interface LauncherActionProvider extends ActionProvider {

    String getScheme();

    AbstractAction createAction(URI uri) throws Exception;
}
