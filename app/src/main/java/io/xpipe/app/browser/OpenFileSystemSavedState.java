package io.xpipe.app.browser;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Jacksonized
@Builder
public class OpenFileSystemSavedState {

    String lastDirectory;
}
