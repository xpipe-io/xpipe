package io.xpipe.app.vnc;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DocumentationLink;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.RemoteDesktopDockEntry;
import io.xpipe.app.util.RemoteDesktopWindow;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.concurrent.atomic.AtomicReference;

@Builder
@Jacksonized
@JsonTypeName("integratedXPipeVncClient")
public class InternalVncClient implements ExternalVncClient {

    @Override
    public void launch(VncLaunchConfig configuration) {
        var w = RemoteDesktopWindow.get();
        w.show();
        var ref = new AtomicReference<RemoteDesktopDockEntry>();
        var session = ProcessControlProvider.get().createVncSession(configuration.getEntry(), () -> {
            w.close(ref.get(), false);
        });
        ref.set(w.trackInternal(
                DataStorage.get().getStoreEntryDisplayName(configuration.getEntry().get()),
                configuration.getEntry().get().getEffectiveIconFile(),
                DataStorage.get().getEffectiveColor(configuration.getEntry().get()),
                configuration.getEntry().get(),
                session));
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
