package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.ext.*;
import io.xpipe.app.process.ShellStoreState;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.HostHelper;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.Validators;
import io.xpipe.ext.base.host.HostAddressGatewayStore;
import io.xpipe.ext.base.service.ServiceAddressRotation;
import io.xpipe.ext.base.service.ServiceProtocolType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@SuperBuilder(toBuilder = true)
@Value
@Jacksonized
@JsonTypeName("scriptSource")
public class ScriptSourceStore implements DataStore, StatefulDataStore<ScriptSourceStore.State> {

    ScriptSource source;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(source);
        source.checkComplete();
    }

    public void refresh() throws Exception {
        source.prepare();
        var l = source.listScripts();
        setState(State.builder().entries(l).build());
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder(toBuilder = true)
    @Jacksonized
    public static class State extends DataStoreState {

        List<ScriptSourceEntry> entries;

        @Override
        public DataStoreState mergeCopy(DataStoreState newer) {
            var s = (State) newer;
            var b = toBuilder();
            b.entries(useNewer(entries, s.entries));
            return b.build();
        }
    }
}
