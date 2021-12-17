package io.xpipe.core.test;

import io.xpipe.core.data.generic.GenericDataStreamParser;
import io.xpipe.core.data.generic.GenericDataStreamWriter;
import io.xpipe.core.data.generic.GenericDataStructureNodeReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;

import static io.xpipe.core.test.DataStructureTests.createTestData;

public class GenericDataStructureIoTest {

    @Test
    public void testBasicIo() throws IOException {
        var obj = createTestData();
        var dataOut = new ByteArrayOutputStream();
        GenericDataStreamWriter.write(dataOut, obj);
        var data = dataOut.toByteArray();

        var format = HexFormat.of().withPrefix("0x").withDelimiter(" ");
        System.out.println(format.formatHex(data));

        var reader = new GenericDataStructureNodeReader();
        GenericDataStreamParser.read(new ByteArrayInputStream(data), reader);
        var node = reader.create();

        Assertions.assertEquals(obj, node);
        System.out.println(node);
    }
}
