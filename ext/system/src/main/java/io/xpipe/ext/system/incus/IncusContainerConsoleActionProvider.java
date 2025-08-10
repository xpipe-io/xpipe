package io.xpipe.ext.system.incus;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class IncusContainerConsoleActionProvider implements HubLeafProvider<IncusContainerStore> {

    @Override
    public AbstractAction createAction(DataStoreEntryRef<IncusContainerStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public boolean requiresValidStore() {
        return false;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<IncusContainerStore> store) {
        return AppI18n.observable("serialConsole");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<IncusContainerStore> store) {
        return new LabelGraphic.IconGraphic("mdi2c-console");
    }

    @Override
    public Class<IncusContainerStore> getApplicableClass() {
        return IncusContainerStore.class;
    }

    @Override
    public String getId() {
        return "openIncusContainerConsole";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<IncusContainerStore> {

        @Override
        public void executeImpl() throws Exception {
            var d = (IncusContainerStore) ref.getStore();
            var view = new IncusCommandView(
                    d.getInstall().getStore().getHost().getStore().getOrStartSession());
            TerminalLaunch.builder()
                    .entry(ref.get())
                    .title("Console")
                    .command(view.console(d.getName()))
                    .launch();
        }
    }
}
