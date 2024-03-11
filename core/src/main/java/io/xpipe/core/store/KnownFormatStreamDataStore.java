package io.xpipe.core.store;

import io.xpipe.core.util.NewLine;
import io.xpipe.core.util.StreamCharset;

public interface KnownFormatStreamDataStore extends StreamDataStore {

    StreamCharset getCharset();

    NewLine getNewLine();
}
