package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Value
@JsonTypeName("string")
public class StringStore implements StreamDataStore {

    byte[] value;

    @JsonCreator
    public StringStore(byte[] value) {
        this.value = value;
    }

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
