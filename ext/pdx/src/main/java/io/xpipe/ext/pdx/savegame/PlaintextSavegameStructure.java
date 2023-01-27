package io.xpipe.ext.pdx.savegame;

import io.xpipe.ext.pdx.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class PlaintextSavegameStructure implements SavegameStructure {

    protected final byte[] header;
    private final String name;
    private final SavegameType type;

    public PlaintextSavegameStructure(byte[] header, String name, SavegameType type) {
        this.header = header;
        this.name = name;
        this.type = type;
    }

    @Override
    public void write(Path out, SavegameContent content) throws IOException {
        try (var partOut = Files.newOutputStream(out)) {
            if (header != null) {
                partOut.write(header);
                partOut.write("\n".getBytes());
            }
            writeData(partOut, content.entrySet().iterator().next().getValue());
        }
    }

    @Override
    public SavegameParseResult parse(byte[] input) {
        if (header != null && !SavegameStructure.validateHeader(header, input)) {
            return new SavegameParseResult.Invalid("File " + name + " has an invalid header");
        }

        try {
            var node = type.getParser().parse(name, input, header != null ? header.length + 1 : 0);
            return new SavegameParseResult.Success(new SavegameContent(Map.of(name, node)));
        } catch (ParseException e) {
            return new SavegameParseResult.Error(e);
        }
    }

    @Override
    public SavegameType getType() {
        return type;
    }
}
