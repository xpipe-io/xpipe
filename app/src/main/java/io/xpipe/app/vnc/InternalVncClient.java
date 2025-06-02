package io.xpipe.app.vnc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserStoreSessionTab;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Builder
@Jacksonized
@JsonTypeName("xpipe")
public class InternalVncClient implements ExternalVncClient {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void launch(LaunchConfiguration configuration) throws Exception {
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

        BrowserFullSessionModel.DEFAULT.openSync(ProcessControlProvider.get().createVncSession(BrowserFullSessionModel.DEFAULT, configuration.getEntry()), null);
        AppLayoutModel.get().selectBrowser();
    }
}
