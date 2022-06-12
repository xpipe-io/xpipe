package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.io.InputStream;

@Value
@JsonTypeName("commandInput")
public class CommandInputStore implements StreamDataStore {

    String cmd;

    @Override
    public InputStream openInput() throws Exception {
        var proc = Runtime.getRuntime().exec(cmd);
        return proc.getInputStream();
    }
}
