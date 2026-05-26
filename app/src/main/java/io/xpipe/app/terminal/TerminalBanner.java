package io.xpipe.app.terminal;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.hub.comp.OsLogoComp;
import io.xpipe.app.process.SystemState;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.AsciiArtConverter;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.core.FilePath;
import lombok.Value;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@Value
public class TerminalBanner {

    DataStoreEntryRef<ShellStore> entry;

    public String build() {
        var iconFile = getEffectiveIcon();
        var iconImage = AppImages.image(FilePath.of(iconFile).getBaseName() + "-24.png");
        var iconAscii = AsciiArtConverter.convert(iconImage);
        var iconLines = iconAscii.lines().toList();

        var name = DataStoreFormatter.cut(DataStorage.get().getStoreEntryDisplayName(entry.get()), 50);
        var sep = "-".repeat(name.length());
        var stats = buildStats();

        var b = new StringBuilder();
        b.append("\n");

        for (int i = 0; i < iconLines.size(); i++) {
            b.append(" ").append(iconLines.get(i)).append("   ");

            if (i == 0) {
                b.append(name);
            } else if (i == 1) {
                b.append(sep);
            } else {
                var statIndex = i - 2;
                if (statIndex < stats.size()) {
                    b.append(stats.get(statIndex));
                }
            }

            b.append("\n");
        }

        b.append("\n");
        return b.toString();
    }

    private String getEffectiveIcon() {
        if (entry.get().getIcon() != null) {
            return entry.get().getEffectiveIconFile();
        }

        if (entry.getStore() instanceof StatefulDataStore<?> sds && sds.getState() instanceof SystemState ss) {
            var os = OsLogoComp.getImage(ss.getOsName(), ss.getOsType());
            return os;
        }

        return entry.get().getEffectiveIconFile();
    }

    private List<String> buildStats() {
        var lines = new ArrayList<String>();
        if (entry.getStore() instanceof StatefulDataStore<?> sds && sds.getState() instanceof SystemState ss) {
            if (ss.getOsName() != null) {
                lines.add(buildKeyValue("OS", ss.getOsName()));
            }
            if (ss.getShellDialect() != null) {
                lines.add(buildKeyValue("Shell", ss.getShellDialect().getDisplayName()));
            }
        }
        return lines;
    }

    private String buildKeyValue(String key, String value) {
        var k = CommandLine.Help.Ansi.AUTO.string("@|yellow " + key + "|@");
        var v = DataStoreFormatter.cut(value, 50);
        return k + ": " + v;
    }
}
