package io.xpipe.ext.system.lxd;

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

public class LxdContainerConsoleActionProvider implements HubLeafProvider<LxdContainerStore> {

    @Override
    public AbstractAction createAction(DataStoreEntryRef<LxdContainerStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public Class<LxdContainerStore> getApplicableClass() {
        return LxdContainerStore.class;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<LxdContainerStore> store) {
        return AppI18n.observable("serialConsole");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<LxdContainerStore> store) {
        return new LabelGraphic.IconGraphic("mdi2c-console");
    }

    @Override
    public boolean requiresValidStore() {
        return false;
    }

    @Override
    public String getId() {
        return "openLxdContainerConsole";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<LxdContainerStore> {

        @Override
        public void executeImpl() throws Exception {
            var d = ref.getStore();
            var view = new LxdCommandView(
                    d.getCmd().getStore().getHost().getStore().getOrStartSession());
            TerminalLaunch.builder()
                    .entry(ref.get())
                    .title("Console")
                    .command(view.console(d.getName()))
                    .launch();
        }
    }
}
