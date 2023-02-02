package io.xpipe.ext.office.docx;

import io.xpipe.core.source.DataSourceConnection;
import io.xpipe.core.source.TextReadConnection;
import io.xpipe.core.store.StreamDataStore;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.util.Arrays;
import java.util.stream.Stream;

public class DocxReadConnection implements TextReadConnection {

    private final StreamDataStore store;

    public DocxReadConnection(StreamDataStore store) {
        this.store = store;
    }

    @Override
    public void init() throws Exception {}

    @Override
    public Stream<String> lines() throws Exception {
        try (XWPFDocument doc = new XWPFDocument(store.openInput())) {

            XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(doc);
            String docText = xwpfWordExtractor.getText();
            return Arrays.stream(docText.split("\r\n"));
        }
    }

    @Override
    public boolean canRead() throws Exception {
        return store.canOpen();
    }

    @Override
    public void forward(DataSourceConnection con) throws Exception {}

    @Override
    public void close() throws Exception {}
}
