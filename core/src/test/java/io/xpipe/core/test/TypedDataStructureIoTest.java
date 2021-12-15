package io.xpipe.core.test;

import io.xpipe.core.data.typed.TypedDataStreamReader;
import io.xpipe.core.data.typed.TypedDataStreamWriter;
import io.xpipe.core.data.typed.TypedDataStructureNodeReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;

import static io.xpipe.core.test.DataStructureTests.createTestData;

public class TypedDataStructureIoTest {

    @Test
    public void testBasicIo() throws IOException {
        var obj = createTestData();
        var type = obj.getDataType();
        var dataOut = new ByteArrayOutputStream();
        TypedDataStreamWriter.writeStructure(dataOut, obj);
        var data = dataOut.toByteArray();

        var format = HexFormat.of().withPrefix("0x").withDelimiter(" ");
        System.out.println(format.formatHex(data));

        var reader = new TypedDataStructureNodeReader(type);
        TypedDataStreamReader.readStructure(new ByteArrayInputStream(data), reader);
        var node = reader.create();

        Assertions.assertEquals(obj, node);
        System.out.println(node);
    }
}
