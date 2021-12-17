package io.xpipe.core.test;

import io.xpipe.core.data.typed.TypedDataStreamParser;
import io.xpipe.core.data.typed.TypedDataStreamWriter;
import io.xpipe.core.data.typed.TypedDataStructureNodeReader;
import io.xpipe.core.data.typed.TypedReusableDataStructureNodeReader;
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
        TypedDataStreamWriter.writeStructure(dataOut, obj, type);
        var data = dataOut.toByteArray();

        var format = HexFormat.of().withPrefix("0x").withDelimiter(" ");
        System.out.println(format.formatHex(data));

        var reader = new TypedDataStructureNodeReader(type);
        new TypedDataStreamParser(type).readStructure(new ByteArrayInputStream(data), reader);
        var node = reader.create();

        Assertions.assertEquals(obj, node);
        System.out.println(node);
    }

    @Test
    public void testBasicReusableIo() throws IOException {
        var obj = createTestData();
        var type = obj.getDataType();
        var dataOut = new ByteArrayOutputStream();
        TypedDataStreamWriter.writeStructure(dataOut, obj, type);
        TypedDataStreamWriter.writeStructure(dataOut, obj, type);
        var data = dataOut.toByteArray();

        var format = HexFormat.of().withPrefix("0x").withDelimiter(" ");
        System.out.println(format.formatHex(data));

        var in = new ByteArrayInputStream(data);
        var reader = new TypedReusableDataStructureNodeReader(type);
        new TypedDataStreamParser(type).readStructure(in, reader);
        var firstNode = reader.create();
        new TypedDataStreamParser(type).readStructure(in, reader);
        var secondNode = reader.create();

        System.out.println(firstNode);
        Assertions.assertEquals(obj, firstNode);
        Assertions.assertEquals(obj, secondNode);
    }
}
