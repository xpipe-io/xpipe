package io.xpipe.ext.pdx.savegame;

import io.xpipe.core.data.node.TupleNode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

public interface SavegameStructure {

    SavegameStructure EU4_PLAINTEXT =
            new PlaintextSavegameStructure("EU4txt".getBytes(), "gamestate", SavegameType.EU4);

    SavegameStructure EU4_COMPRESSED = new ZipSavegameStructure(
            "EU4txt".getBytes(),
            SavegameType.EU4,
            Set.of(
                    new ZipSavegameStructure.SavegamePart("ai", "ai"),
                    new ZipSavegameStructure.SavegamePart("meta", "meta"),
                    new ZipSavegameStructure.SavegamePart("gamestate", "gamestate")),
            "rnw.zip");

    SavegameStructure CK3_PLAINTEXT = new Ck3PlaintextSavegameStructure();

    SavegameStructure CK3_COMPRESSED = new Ck3CompressedSavegameStructure();

    SavegameStructure HOI4 = new PlaintextSavegameStructure("HOI4txt".getBytes(), "gamestate", SavegameType.HOI4);

    SavegameStructure STELLARIS = new ZipSavegameStructure(
            null,
            SavegameType.STELLARIS,
            Set.of(
                    new ZipSavegameStructure.SavegamePart("meta", "meta"),
                    new ZipSavegameStructure.SavegamePart("gamestate", "gamestate")));

    SavegameStructure CK2_PLAINTEXT =
            new PlaintextSavegameStructure("CK2txt".getBytes(), "gamestate", SavegameType.CK2) {

                @Override
                public void writeData(OutputStream out, TupleNode node) throws IOException {
                    NodeWriter.write(out, getType().getParser().getCharset(), node, "\t", 1);
                    out.write("}".getBytes());
                }
            };

    SavegameStructure CK2_COMPRESSED =
            new ZipSavegameStructure(
                    "CK2txt".getBytes(),
                    SavegameType.CK2,
                    Set.of(
                            new ZipSavegameStructure.SavegamePart("meta", "meta"),
                            new ZipSavegameStructure.SavegamePart("gamestate", "*"))) {

                @Override
                public void writeData(OutputStream out, TupleNode node) throws IOException {
                    NodeWriter.write(out, getType().getParser().getCharset(), node, "\t", 1);
                    out.write("\n}".getBytes());
                }
            };

    SavegameStructure VIC2 = new PlaintextSavegameStructure(null, "gamestate", SavegameType.VIC2) {

        @Override
        public void writeData(OutputStream out, TupleNode node) throws IOException {
            NodeWriter.write(out, getType().getParser().getCharset(), node, "\t", 0);
            out.write("}".getBytes());
        }
    };

    static boolean validateHeader(byte[] header, byte[] content) {
        if (content.length < header.length) {
            return false;
        }

        byte[] first = new byte[header.length];
        System.arraycopy(content, 0, first, 0, header.length);
        return Arrays.equals(first, header);
    }

    void write(Path out, SavegameContent content) throws IOException;

    default void writeData(OutputStream out, TupleNode node) throws IOException {
        NodeWriter.write(out, getType().getParser().getCharset(), node, "\t", 0);
    }

    SavegameParseResult parse(byte[] input);

    SavegameType getType();
}
