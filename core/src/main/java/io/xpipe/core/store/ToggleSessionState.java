package io.xpipe.core.store;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@SuperBuilder
@Jacksonized
public class ToggleSessionState extends DataStoreState {

        Boolean enabled;
        Boolean running;

        @Override
        public void merge(DataStoreState newer) {
            var state = (ToggleSessionState) newer;
            enabled = useNewer(enabled, state.enabled);
            running = useNewer(running, state.running);
        }
    }