package io.xpipe.app.action;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface LauncherUrlProvider extends ActionProvider {

    static List<LauncherUrlProvider> getAll() {
        return ActionProvider.ALL.stream()
                .map(actionProvider -> actionProvider instanceof LauncherUrlProvider lup ? lup : null)
                .filter(Objects::nonNull)
                .toList();
    }

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
