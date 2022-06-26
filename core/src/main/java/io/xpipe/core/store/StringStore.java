package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Value
@JsonTypeName("string")
@AllArgsConstructor
public class StringStore implements StreamDataStore {

    byte[] value;

    public StringStore(String s) {
        value = s.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public InputStream openInput() throws Exception {
        return new ByteArrayInputStream(value);
    }

    @Override
    public String toDisplay() {
        return "string";
    }
}
