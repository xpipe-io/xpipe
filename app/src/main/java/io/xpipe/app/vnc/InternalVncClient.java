package io.xpipe.app.vnc;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserStoreSessionTab;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ProcessControlProvider;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@JsonTypeName("integratedXPipeVncClient")
public class InternalVncClient implements ExternalVncClient {

    @Override
    public boolean supportsPasswords() {
        return true;
    }

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        var open = BrowserFullSessionModel.DEFAULT.getSessionEntriesSnapshot().stream()
                .filter(browserSessionTab -> browserSessionTab instanceof BrowserStoreSessionTab<?> st
                        && st.getEntry().get().equals(configuration.getEntry().get()))
                .findFirst()
                .orElse(null);
        if (open != null) {
            AppLayoutModel.get().selectBrowser();
            BrowserFullSessionModel.DEFAULT.getSelectedEntry().setValue(open);
            return;
        }

        BrowserFullSessionModel.DEFAULT.openSync(
                ProcessControlProvider.get()
                        .createVncSession(BrowserFullSessionModel.DEFAULT, configuration.getEntry()),
                null);
        AppLayoutModel.get().selectBrowser();
    }
}
