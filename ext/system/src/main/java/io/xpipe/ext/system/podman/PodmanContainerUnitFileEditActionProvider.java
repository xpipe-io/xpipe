package io.xpipe.ext.system.podman;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.FilePath;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.regex.Pattern;

public class PodmanContainerUnitFileEditActionProvider implements HubLeafProvider<PodmanContainerStore> {

    @Override
    public AbstractAction createAction(DataStoreEntryRef<PodmanContainerStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<PodmanContainerStore> store) {
        return AppI18n.observable("editContainerUnitFile");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<PodmanContainerStore> store) {
        return new LabelGraphic.IconGraphic("mdi2f-file-document-edit");
    }

    @Override
    public Class<PodmanContainerStore> getApplicableClass() {
        return PodmanContainerStore.class;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<PodmanContainerStore> o) {
        return o.getStore().getState().getSystemdUnit() != null;
    }

    @Override
    public String getId() {
        return "editPodmanContainerUnitFile";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<PodmanContainerStore> {

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            var host = ref.getStore().getCmd().getStore().getHost();
            var unitFileName = ref.getStore().getState().getSystemdUnit();
            var sc = host.getStore().getOrStartSession();
            var runtimeDir = FilePath.of(sc.view().getEnvironmentVariableOrThrow("XDG_RUNTIME_DIR"));
            var unitFile = runtimeDir.join("systemd", "generator", unitFileName);
            if (!sc.view().fileExists(unitFile)) {
                throw ErrorEventFactory.expected(new IllegalArgumentException(
                        "Container unit file " + unitFileName + " not found at " + unitFile));
            }

            var content = sc.view().readTextFile(unitFile);
            var matcher = Pattern.compile("SourcePath=(.+)").matcher(content);
            if (!matcher.find()) {
                throw ErrorEventFactory.expected(new IllegalArgumentException(
                        "Source of container unit file " + unitFileName + " not locatable from file " + unitFile));
            }

            var sourceFile = FilePath.of(matcher.group(1));
            FileOpener.openString(
                    sourceFile.getFileName(),
                    sourceFile.getFileName(),
                    sc.view().readTextFile(sourceFile),
                    s -> {
                        try {
                            sc.view().writeTextFile(sourceFile, s);
                        } catch (Exception e) {
                            ErrorEventFactory.fromThrowable(e).handle();
                        }
                    });
        }
    }
}
