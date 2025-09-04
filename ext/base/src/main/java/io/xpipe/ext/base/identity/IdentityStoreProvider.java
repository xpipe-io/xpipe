package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.StoreEntryWrapper;
import io.xpipe.app.hub.comp.StoreSection;
import io.xpipe.app.hub.comp.SystemStateComp;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.ext.base.identity.ssh.NoneStrategy;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class IdentityStoreProvider implements DataStoreProvider {

    @Override
    public List<String> getSearchableTerms(DataStore store) {
        IdentityStore s = store.asNeeded();
        var name = s.getUsername().getFixedUsername();
        return name.isPresent() ? List.of(name.get()) : List.of();
    }

    @Override
    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.IDENTITY;
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.IDENTITY;
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        var st = (IdentityStore) section.getWrapper().getStore().getValue();
        var user = st.getUsername().hasUser()
                ? st.getUsername().getFixedUsername().map(s -> "User " + s).orElse("User")
                : "Anonymous User";
        var s = user
                + (st.getPassword() == null || st.getPassword() instanceof SecretNoneStrategy ? "" : " + Password")
                + (st.getSshIdentity() == null || st.getSshIdentity() instanceof NoneStrategy ? "" : " + Key");
        return new SimpleStringProperty(s);
    }
}
