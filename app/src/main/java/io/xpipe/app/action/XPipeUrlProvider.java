package io.xpipe.app.action;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
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

        if ("webtop".equals(a)) {
            ProcModuleProvider.get().openWebtopUrl(uri);
            return null;
        }

        if ("action".equals(a)) {
            if (!checkEnabled()) {
                return null;
            }

            var query = uri.getRawQuery();
            var action = ActionUrls.parse(query);

            if (action.isPresent() && !action.get().requiresConfirmation()) {
                var r = ActionConfirmation.confirmAction(action.get());
                if (!r) {
                    return null;
                }
            }

            return action.orElse(null);
        }

        if ("sync".equals(a)) {
            if (!checkEnabled()) {
                return null;
            }

            var repo = new String(Base64Helper.fromBase64UrlString(uri.getPath()), StandardCharsets.UTF_8);
            var currentRepo = AppPrefs.get().storageGitRemote().getValue();
            var alreadySynced = currentRepo != null && !currentRepo.isBlank();
            if (alreadySynced && !repo.equals(currentRepo)) {
                AppDialog.information("syncUrlAlreadySynced");
                return null;
            }

            var confirm = AppDialog.confirm("syncUrlSetTitle", AppI18n.observable("syncUrlSetContent", repo));
            if (!confirm) {
                return null;
            }

            AppPrefs.get().setFromExternal(AppPrefs.get().storageGitRemote(), repo);
            AppPrefs.get().setFromExternal(AppPrefs.get().enableGitStorage(), true);
            AppPrefs.get().save();
            AppRestart.restart();
            return null;
        }

        return null;
    }

    private boolean checkEnabled() {
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
