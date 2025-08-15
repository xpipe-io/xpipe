package io.xpipe.ext.base.service;

import io.xpipe.app.ext.Session;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ElevationFunction;

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

    public boolean isRunning() {
        return true;
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
    public boolean checkAlive() {
        return true;
    }
}
