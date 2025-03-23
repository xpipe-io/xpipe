package io.xpipe.app.password;

import io.xpipe.core.util.InPlaceSecretValue;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
public class KeePassXcAssociationKey {
    String id;
    InPlaceSecretValue key;
}
