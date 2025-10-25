package io.xpipe.app.vnc;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserStoreSessionTab;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.util.DocumentationLink;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@JsonTypeName("integratedXPipeVncClient")
public class InternalVncClient implements ExternalVncClient {

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        var browserSession = BrowserFullSessionModel.DEFAULT;
        var open = browserSession.getSessionEntriesSnapshot().stream()
                .filter(browserSessionTab -> browserSessionTab instanceof BrowserStoreSessionTab<?> st
                        && st.getEntry().get().equals(configuration.getEntry().get()))
                .findFirst()
                .orElse(null);
        if (open != null) {
            AppLayoutModel.get().selectBrowser();
            browserSession.getSelectedEntry().setValue(open);
            return;
        }

        browserSession.openSync(
                ProcessControlProvider.get().createVncSession(browserSession, configuration.getEntry()),
                browserSession.getBusy());
        AppLayoutModel.get().selectBrowser();
    }

    @Override
    public boolean supportsPasswords() {
        return true;
    }

    @Override
    public String getWebsite() {
        return DocumentationLink.VNC_CLIENTS.getLink();
    }
}
