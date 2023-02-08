package io.xpipe.ext.proc.store;

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

public class DockerStoreProvider implements DataStoreProvider {

    @Override
    public DataStore getParent(DataStore store) {
        DockerStore s = store.asNeeded();
        return s.getHost();
    }

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        DockerStore st = (DockerStore) store.getValue();

        Property<ShellStore> shellProp =
                new SimpleObjectProperty<>(st.getHost() != null ? st.getHost() : new LocalStore());
        Property<String> containerProp = new SimpleObjectProperty<>(st.getContainerName());

        var q = new DynamicOptionsBuilder(I18n.observable("configuration"))
                .addComp(
                        I18n.observable("host"),
                        ShellStoreChoiceComp.host(st, shellProp),
                        shellProp)
                .nonNull(val)
                .addString(I18n.observable("proc.container"), containerProp)
                .nonNull(val)
                .bind(
                        () -> {
                            return new DockerStore(shellProp.getValue(), containerProp.getValue());
                        },
                        store)
                .buildComp();
        return new GuiDialog(q, val);
    }

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        DockerStore s = store.asNeeded();
        return String.format("%s %s", s.queryMachineName(), I18n.get("proc.container"));
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        DockerStore s = store.asNeeded();
        return DataStoreFormatter.formatSubHost(
                l -> DataStoreFormatter.cut(s.getContainerName(), l), s.getHost(), length);
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(DockerStore.class);
    }

    @Override
    public DataStore defaultStore() {
        return new DockerStore(new LocalStore(), null);
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        DockerStore dockerStore = store.asNeeded();
        var hostQ = DialogHelper.machineQuery(dockerStore.getHost());
        var containerQ =
                Dialog.query("Container", false, true, false, dockerStore.getContainerName(), QueryConverter.STRING);
        return Dialog.chain(hostQ, containerQ).evaluateTo(() -> {
            return new DockerStore(hostQ.getResult(), containerQ.getResult());
        });
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("docker", "docker_container");
    }
}
