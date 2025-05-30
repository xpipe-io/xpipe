package io.xpipe.ext.base.service;

import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ElevationFunction;
import io.xpipe.app.ext.Session;
import io.xpipe.app.ext.SessionListener;

import lombok.Getter;

@Getter
public class ServiceControlSession extends Session {

    private final ServiceControlStore store;

    protected ServiceControlSession(ServiceControlStore store) {
        this.store = store;
    }

    private ElevationFunction elevationFunction() {
        return store.isElevated() ? ElevationFunction.elevated("service") : ElevationFunction.none();
    }

    public void start() throws Exception {
        if (isRunning()) {
            listener.onStateChange(true);
            return;
        }

        var session = store.getHost().getStore().getOrStartSession();
        var builder = session.getShellDialect()
                .launchAsnyc(CommandBuilder.of().add(store.getStartScript().getValue()));
        session.command(builder).elevated(elevationFunction()).execute();
        listener.onStateChange(true);
    }

    public boolean isRunning() {
        return true;
    }

    public void stop() throws Exception {
        if (!isRunning()) {
            listener.onStateChange(false);
            return;
        }

        var session = store.getHost().getStore().getOrStartSession();
        session.command(store.getStopScript()).elevated(elevationFunction()).execute();
        listener.onStateChange(false);
    }

    @Override
    public boolean checkAlive() throws Exception {
        return true;
    }
}
