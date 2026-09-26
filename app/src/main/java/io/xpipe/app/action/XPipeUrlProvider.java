package io.xpipe.app.action;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.ProcModuleProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.Base64Helper;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class XPipeUrlProvider implements LauncherUrlProvider {

    @Override
    public String getScheme() {
        return "xpipe";
    }

    @Override
    public AbstractAction createAction(URI uri) throws Exception {
        var a = uri.getHost();

        if (a.equals("webtop")) {
            ProcModuleProvider.get().openWebtopUrl(uri);
            return null;
        }

        if ("action".equals(a)) {
            if (!checkPermission()) {
                return null;
            }

            var query = uri.getQuery();
            var action = ActionUrls.parse(query);
            return action.orElse(null);
        }

        if ("sync".equals(a)) {
            if (!checkPermission()) {
                return null;
            }

            var repo = new String(Base64Helper.fromBase64UrlString(uri.getPath()), StandardCharsets.UTF_8);
            var alreadySynced = AppPrefs.get().storageGitRemote().getValue() != null;
            if (alreadySynced && !repo.equals(AppPrefs.get().storageGitRemote().getValue())) {
                AppDialog.information("syncUrlAlreadySynced");
                return null;
            }

            AppPrefs.get().setFromExternal(AppPrefs.get().storageGitRemote(), repo);
            AppPrefs.get().save();
            AppRestart.restart();
            return null;
        }

        return null;
    }

    private boolean checkPermission() {
        var cache = AppCache.getBoolean("externalUrlsPermitted", false);
        if (cache) {
            return true;
        }

        var r = AppDialog.confirm("externalUrl");
        if (r) {
            AppCache.update("externalUrlsPermitted", true);
        }
        return r;
    }
}
