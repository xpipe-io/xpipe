package io.xpipe.core.store;

import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;

public interface KnownFormatStreamDataStore extends StreamDataStore {

    StreamCharset getCharset();

    NewLine getNewLine();
}
