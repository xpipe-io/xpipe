package io.xpipe.app.prefs;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
public class KeePassXcAssociationKey {
    String id;
    String key;
    String hash;
}
