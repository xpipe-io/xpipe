package io.xpipe.app.action;

import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.ProcModuleProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.Base64Helper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.webtop.WebtopApp;
import io.xpipe.app.webtop.WebtopAppListManager;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

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
            var query = uri.getQuery();
            var action = ActionUrls.parse(query);
            return action.orElse(null);
        }

        if ("sync".equals(a)) {
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
}
