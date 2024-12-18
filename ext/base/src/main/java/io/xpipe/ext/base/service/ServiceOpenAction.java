package io.xpipe.ext.base.service;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import lombok.Value;

import java.util.List;

public class ServiceOpenAction implements ActionProvider {

    @Override
    public BranchDataStoreCallSite<?> getBranchDataStoreCallSite() {
        return new BranchDataStoreCallSite<>() {
            @Override
            public boolean isMajor(DataStoreEntryRef<DataStore> o) {
                return true;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("openWebsite");
            }

            @Override
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdi2s-search-web";
            }

            @Override
            public Class<AbstractServiceStore> getApplicableClass() {
                return AbstractServiceStore.class;
            }

            @Override
            public List<ActionProvider> getChildren(DataStoreEntryRef<DataStore> store) {
                return List.of(new HttpAction(), new HttpsAction());
            }
        };
    }

    private static class HttpAction implements ActionProvider {

        @Override
        public String getId() {
            return "serviceOpenHttp";
        }

        @Override
        public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
            return new LeafDataStoreCallSite<AbstractServiceStore>() {

                @Override
                public boolean canLinkTo() {
                    return true;
                }

                @Override
                public ActionProvider.Action createAction(DataStoreEntryRef<AbstractServiceStore> store) {
                    return new ServiceOpenAction.Action("http", store.getStore());
                }

                @Override
                public Class<AbstractServiceStore> getApplicableClass() {
                    return AbstractServiceStore.class;
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<AbstractServiceStore> store) {
                    return AppI18n.observable("openHttp");
                }

                @Override
                public String getIcon(DataStoreEntryRef<AbstractServiceStore> store) {
                    return "mdi2s-shield-off-outline";
                }
            };
        }
    }

    private static class HttpsAction implements ActionProvider {

        @Override
        public String getId() {
            return "serviceOpenHttps";
        }

        @Override
        public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
            return new LeafDataStoreCallSite<AbstractServiceStore>() {

                @Override
                public boolean canLinkTo() {
                    return true;
                }

                @Override
                public ActionProvider.Action createAction(DataStoreEntryRef<AbstractServiceStore> store) {
                    return new ServiceOpenAction.Action("https", store.getStore());
                }

                @Override
                public Class<AbstractServiceStore> getApplicableClass() {
                    return AbstractServiceStore.class;
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<AbstractServiceStore> store) {
                    return AppI18n.observable("openHttps");
                }

                @Override
                public String getIcon(DataStoreEntryRef<AbstractServiceStore> store) {
                    return "mdi2s-shield-lock-outline";
                }
            };
        }
    }

    @Value
    static class Action implements ActionProvider.Action {

        String protocol;
        AbstractServiceStore serviceStore;

        @Override
        public void execute() throws Exception {
            serviceStore.startSessionIfNeeded();
            var l = serviceStore.getSession().getLocalPort();
            var path =
                    serviceStore.getPath() != null && !serviceStore.getPath().isEmpty() ? serviceStore.getPath() : "";
            if (!path.isEmpty() && !path.startsWith("/")) {
                path = "/" + path;
            }
            Hyperlinks.open(protocol + "://localhost:" + l + path);
        }
    }
}
