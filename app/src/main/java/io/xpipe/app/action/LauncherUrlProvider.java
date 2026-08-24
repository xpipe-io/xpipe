package io.xpipe.app.action;

import java.net.URI;
import java.util.Optional;

public interface LauncherUrlProvider extends ActionProvider {

    static Optional<LauncherUrlProvider> find(String url) {
        return ActionProvider.ALL.stream()
                .filter(actionProvider -> actionProvider instanceof LauncherUrlProvider lup
                        && url.toLowerCase().startsWith(lup.getScheme().toLowerCase() + ":"))
                .findFirst()
                .map(lup -> (LauncherUrlProvider) lup);
    }

    String getScheme();

    AbstractAction createAction(URI uri) throws Exception;
}
