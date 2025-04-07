package io.xpipe.app.storage;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class DataStoreCategoryConfig {

    DataStoreColor color;

    Boolean allowInitScripts;

    Boolean allowTerminalPromptScripts;
}
