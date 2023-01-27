package io.xpipe.ext.pdx.savegame;

import io.xpipe.core.data.node.TupleNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipSavegameStructure implements SavegameStructure {

    private final byte[] header;
    private final SavegameType type;
    private final Set<SavegamePart> parts;
    private final String[] ignored;

    public ZipSavegameStructure(byte[] header, SavegameType type, Set<SavegamePart> parts, String... ignored) {
        this.header = header;
        this.type = type;
        this.parts = parts;
        this.ignored = ignored;
    }

    public static byte[] getFirstHeader(byte[] input, String zipFile, int maxLength) {
        try {
            var zipIn = new ZipInputStream(new ByteArrayInputStream(input));
            if (zipIn.getNextEntry() != null) {
                return Arrays.copyOfRange(zipIn.readAllBytes(), 0, maxLength);
            }
        } catch (IOException ignored) {
        }

        return Arrays.copyOfRange(input, 0, maxLength);
    }

    protected SavegameParseResult parseInput(byte[] input, int offset) {
        var wildcard = parts.stream().filter(p -> p.identifier().equals("*")).findAny();

        try {
            try (var zipIn = new ZipInputStream(new ByteArrayInputStream(input, offset, input.length - offset))) {
                Map<String, TupleNode> nodes = new HashMap<>();
                ZipEntry entry;
                while ((entry = zipIn.getNextEntry()) != null) {
                    ZipEntry finalEntry = entry;

                    // Skip ignored entries
                    if (Arrays.stream(ignored).anyMatch(s -> s.equals(finalEntry.getName()))) {
                        continue;
                    }

                    var part = parts.stream()
                            .filter(p -> p.identifier().equals(finalEntry.getName()))
                            .findAny()
                            .or(() -> wildcard);

                    // Ignore unknown entry
                    if (part.isEmpty()) {
                        continue;
                    }

                    var bytes = zipIn.readAllBytes();
                    if (header != null && !SavegameStructure.validateHeader(header, bytes)) {
                        return new SavegameParseResult.Invalid(
                                "File " + part.get().identifier() + " has an invalid header");
                    }

                    var node = type.getParser().parse(part.get().name, bytes, header != null ? header.length + 1 : 0);
                    nodes.put(part.get().name(), node);
                }

                var missingParts = parts.stream()
                        .map(part -> part.name())
                        .filter(s -> !nodes.containsKey(s))
                        .toList();
                if (missingParts.size() > 0) {
                    return new SavegameParseResult.Invalid("Missing parts: " + String.join(", ", missingParts));
                }

                return new SavegameParseResult.Success(new SavegameContent(nodes));
            }
        } catch (Exception t) {
            return new SavegameParseResult.Error(t);
        }
    }

    @Override
    public void write(Path out, SavegameContent content) throws IOException {
        try (var fs = FileSystems.newFileSystem(out)) {
            for (var e : content.entrySet()) {
                var usedPart = parts.stream()
                        .filter(part -> part.name().equals(e.getKey()))
                        .findAny();
                if (usedPart.isEmpty()) {
                    continue;
                }

                var path = fs.getPath(usedPart.get().identifier());
                try (var partOut = Files.newOutputStream(path)) {
                    if (header != null) {
                        partOut.write(header);
                        partOut.write("\n".getBytes());
                    }
                    NodeWriter.write(partOut, type.getParser().getCharset(), e.getValue(), "\t", 0);
                }
            }
        }
    }

    @Override
    public SavegameParseResult parse(byte[] input) {
        return parseInput(input, 0);
    }

    @Override
    public SavegameType getType() {
        return type;
    }

    public record SavegamePart(String name, String identifier) {}
}
