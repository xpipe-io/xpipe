package io.xpipe.ext.collections;

import io.xpipe.core.store.StreamDataStore;

import java.io.InputStream;
import java.io.OutputStream;

public class ArchiveEntryStore extends ArchiveEntryDataStore {

    private final ArchiveReadConnection con;

    public ArchiveEntryStore(StreamDataStore parent, boolean dir, ArchiveReadConnection con, String name) {
        super(dir, name, parent);
        this.con = con;
    }

    @Override
    public InputStream openInput() {
        return con.getInputStream();
    }

    @Override
    public OutputStream openOutput() {
        return null;
    }

    @Override
    public boolean canOpen() {
        return true;
    }
}
