package io.xpipe.cli.source;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.asciithemes.a7.A7_Grids;
import io.xpipe.beacon.exchange.QueryDataSourceExchange;
import io.xpipe.beacon.exchange.api.QueryRawDataExchange;
import io.xpipe.beacon.exchange.api.QueryTableDataExchange;
import io.xpipe.beacon.exchange.api.QueryTextDataExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.CliHelper;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.SourceRefMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.typed.TypedDataStreamParser;
import io.xpipe.core.data.typed.TypedDataStructureNodeReader;
import picocli.CommandLine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static picocli.CommandLine.Help.Visibility.ALWAYS;

@CommandLine.Command(
        name = "peek",
        header = "Peeks and displays the contents of a data source",
        description =
                "Displays the first few lines of content of a data source. "
                        + "Only the actual content that is displayed is actually queried, i.e. the complete data source is not read.",
        sortOptions = false)
public class SourcePeekCommand extends BaseCommand {

    @CommandLine.Option(
            names = {"-l", "--lines"},
            description = "The maximum amount of lines to use to display the data source content",
            showDefaultValue = ALWAYS,
            paramLabel = "<lines>")
    int maxLines = 10;

    @CommandLine.Mixin
    SourceRefMixin source;

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        var req = QueryDataSourceExchange.Request.builder().ref(source.ref).build();
        QueryDataSourceExchange.Response infoRes = con.performSimpleExchange(req);
        var type = infoRes.getType();
        switch (type) {
            case TABLE -> {
                peekTable(con);
            }
            case STRUCTURE -> {
                peekStructure(con);
            }
            case TEXT -> {
                peekText(con);
            }
            case RAW -> {
                peekRaw(con);
            }
            case COLLECTION -> {
                peekCollection(con);
            }
        }
    }

    private void peekRaw(XPipeCliConnection con) {
        int w = CliHelper.getConsoleWidth();
        int bytesPerLine = w / 5;
        int byteCount = bytesPerLine * maxLines;

        var req = QueryRawDataExchange.Request.builder()
                .ref(source.ref)
                .maxBytes(byteCount)
                .build();
        con.performInputExchange(req, (QueryRawDataExchange.Response res, InputStream in) -> {
            int b;
            var format = HexFormat.of().withPrefix("0x");
            int cursor = 0;
            int lines = 0;
            while ((b = in.read()) != -1 && lines <= maxLines) {
                var s = format.formatHex(new byte[] {(byte) b});
                if (cursor + s.length() + 1 > w) {
                    cursor = 0;
                    lines++;
                    System.out.println();
                }

                var toPrint = (cursor == 0 ? "" : " ") + s;
                System.out.print(toPrint);
                cursor += toPrint.length();
            }
            System.out.println(" ...");
        });
    }

    private void peekText(XPipeCliConnection con) {
        var req = QueryTextDataExchange.Request.builder()
                .ref(source.ref)
                .maxLines(maxLines)
                .build();
        con.performInputExchange(req, (QueryTextDataExchange.Response res, InputStream in) -> {
            var r = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(r);
        });
    }

    private void peekCollection(XPipeCliConnection con) {}

    private void peekStructure(XPipeCliConnection con) {}

    private void peekTable(XPipeCliConnection con) {
        int rows = Math.max(maxLines - 2, 0);
        var req = QueryTableDataExchange.Request.builder()
                .ref(source.ref)
                .maxRows(rows)
                .build();

        AsciiTable at = new AsciiTable();
        con.performInputExchange(req, (QueryTableDataExchange.Response res, InputStream in) -> {
            at.addRow(res.getDataType().getNames().stream()
                    .map(s -> s != null ? s : "Unnamed")
                    .toList());
            at.addRule();

            var r = new TypedDataStreamParser(res.getDataType());
            r.parseStructures(in, TypedDataStructureNodeReader.of(res.getDataType()), node -> {
                at.addRow(node.getNodes().stream()
                        .map(DataStructureNode::asString)
                        .toList());
            });
        });

        at.getContext().setGrid(A7_Grids.minusBarPlusEquals());
        at.getRenderer().setCWC(new CWC_LongestLine());
        at.setPaddingLeft(1);
        at.setPaddingRight(1);
        System.out.println(at.render());
    }
}
