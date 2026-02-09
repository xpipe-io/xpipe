package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.hub.action.MultiStoreAction;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.*;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class OpenSplitHubBatchProvider implements BatchHubProvider<ShellStore> {

    @Override
    public boolean isActive(DataStoreEntryRef<ShellStore> o) {
        return TerminalSplitStrategy.getEffectiveSplitStrategyObservable().getValue() != null;
    }

    @Override
    public Class<ShellStore> getApplicableClass() {
        return ShellStore.class;
    }

    @Override
    public String getId() {
        return "openSplit";
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("openSplit");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-console-network-outline");
    }

    @Override
    public AbstractAction createBatchAction(List<DataStoreEntryRef<ShellStore>> dataStoreEntryRefs) {
        return Action.builder().refs(dataStoreEntryRefs).build();
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends MultiStoreAction<ShellStore> {

        @Override
        public void executeImpl() throws Exception {
            var type = AppPrefs.get().terminalType().getValue();
            if (type == null) {
                throw ErrorEventFactory.expected(new IllegalStateException(AppI18n.get("noTerminalSet")));
            }

            var latch = new CountDownLatch(getRefs().size());
            var panes = new TerminalLauncher.Config[getRefs().size()];
            for (int i = 0; i < getRefs().size(); i++) {
                var ii = i;
                var ref = refs.get(i);
                ThreadHelper.runFailableAsync(() -> {
                    try {
                        var replacement = ProcessControlProvider.get().replace(ref);
                        ShellStore store = replacement.getStore().asNeeded();
                        var control = store.standaloneControl();
                        // These prepend scripts, not append
                        TerminalPromptManager.configurePromptScript(control);
                        ProcessControlProvider.get().withDefaultScripts(control);

                        var title = DataStorage.get().getStoreEntryDisplayName(ref.get());
                        var config = new TerminalLauncher.Config(
                                ref.get(), title, null, UUID.randomUUID(), true, true, control);
                        panes[ii] = config;
                    } finally {
                        latch.countDown();
                    }
                });
            }
            TerminalLauncher.open(Arrays.asList(panes), true, type);
        }
    }
}
