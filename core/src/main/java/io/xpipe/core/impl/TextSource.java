package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.xpipe.core.charsetter.Charsettable;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.source.TextDataSource;
import io.xpipe.core.store.StreamDataStore;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class TextSource extends TextDataSource<StreamDataStore> implements Charsettable {

    StreamCharset charset;
    NewLine newLine;

    public TextSource(StreamDataStore store){
        this(store, StreamCharset.UTF8, NewLine.LF);
    }

   @JsonCreator
    public TextSource(StreamDataStore store, StreamCharset charset, NewLine newLine) {
        super(store);
        this.charset = charset;
        this.newLine = newLine;
    }

    @Override
    protected io.xpipe.core.source.TextWriteConnection newWriteConnection() {
        return new TextWriteConnection(this);
    }

    @Override
    protected io.xpipe.core.source.TextWriteConnection newAppendingWriteConnection() {
        return new PreservingTextWriteConnection(this, newWriteConnection(), true);
    }

    @Override
    protected io.xpipe.core.source.TextReadConnection newReadConnection() {
        return new TextReadConnection(this);
    }
}
