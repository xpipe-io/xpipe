package io.xpipe.ext.proc;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.GuiDialog;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.impl.ShellStoreChoiceComp;
import io.xpipe.extension.util.DataStoreFormatter;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.SimpleValidator;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class WslStoreProvider implements DataStoreProvider {

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        WslStore st = (WslStore) store.getValue();
        Property<ShellStore> shellProp = new SimpleObjectProperty<>(st.getHost());
        Property<String> distProp = new SimpleObjectProperty<>(st.getDistribution());
        shellProp.addListener((observable, oldValue, newValue) -> {
            distProp.setValue(WslStore.getDefaultDistribution(newValue).orElse(null));
        });

        Property<String> userProp = new SimpleObjectProperty<>(st.getUser());
        var q = new DynamicOptionsBuilder(I18n.observable("configuration"))
                .addComp(
                        I18n.observable("host"),
                        ShellStoreChoiceComp.host(st, shellProp),
                        shellProp)
                .nonNull(val)
                .addString(I18n.observable("proc.distribution"), distProp)
                .nonNull(val)
                .addString(I18n.observable("proc.username"), userProp)
                .nonNull(val)
                .bind(
                        () -> {
                            return new WslStore(shellProp.getValue(), distProp.getValue(), userProp.getValue());
                        },
                        store)
                .buildComp();
        return new GuiDialog(q, val);
    }

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        WslStore s = store.asNeeded();
        return String.format("%s %s", "WSL", s.queryMachineName());
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        WslStore s = store.asNeeded();
        return DataStoreFormatter.formatSubHost(
                value -> DataStoreFormatter.cut(s.getUser() + "@" + s.getDistribution(), value), s.getHost(), length);
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(WslStore.class);
    }

    @Override
    public DataStore defaultStore() {
        return new WslStore(
                new LocalStore(),
                WslStore.getDefaultDistribution(new LocalStore()).orElse(null),
                "root");
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("wsl");
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        WslStore wslStore = store.asNeeded();
        var hostQ = DialogHelper.shellQuery("WSL Host", wslStore.getHost());
        var distQ = Dialog.lazy(() -> Dialog.query(
                "Distribution",
                false,
                true,
                false,
                wslStore.getDistribution() != null
                        ? wslStore.getDistribution()
                        : WslStore.getDefaultDistribution(hostQ.getResult()).orElse(null),
                QueryConverter.STRING));
        var userQ = Dialog.query("Username", false, true, false, wslStore.getUser(), QueryConverter.STRING);
        return Dialog.chain(hostQ, distQ, userQ).evaluateTo(() -> {
            return new WslStore(hostQ.getResult(), distQ.getResult(), userQ.getResult());
        });
    }

    @Override
    public String getDisplayIconFileName() {
        return "proc:wsl_icon.svg";
    }
}
