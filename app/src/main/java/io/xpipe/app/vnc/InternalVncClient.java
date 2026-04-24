package io.xpipe.app.vnc;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserStoreSessionTab;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DocumentationLink;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.RemoteDesktopWindow;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@JsonTypeName("integratedXPipeVncClient")
public class InternalVncClient implements ExternalVncClient {

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        var w = RemoteDesktopWindow.get();
        w.show();
        var session = ProcessControlProvider.get().createVncSession(configuration.getEntry(), w.getLocked());
        w.trackInternal(DataStorage.get().getStoreEntryDisplayName(configuration.getEntry().get()),
                configuration.getEntry().get().getEffectiveIconFile(),
                DataStorage.get().getEffectiveColor(configuration.getEntry().get()),
                configuration.getEntry().get(),
                session);
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
