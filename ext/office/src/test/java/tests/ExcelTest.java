package tests;

import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.ext.office.excel.ExcelSource;
import io.xpipe.ext.office.excel.model.ExcelCellLocation;
import io.xpipe.ext.office.excel.model.ExcelHeaderState;
import io.xpipe.ext.office.excel.model.ExcelRange;
import io.xpipe.ext.office.excel.model.ExcelSheetIdentifier;
import io.xpipe.extension.util.DaemonExtensionTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;

public class ExcelTest extends DaemonExtensionTest {

    @Test
    public void testEmpty() throws Exception {
        var source = getSource("excel", "empty.xlsx").asTable();
        var lines = source.readAll();

        Assertions.assertEquals(lines.size(), 0);

        ExcelSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(ExcelHeaderState.INCLUDED, detected.getHeaderState());
        Assertions.assertEquals(ExcelSheetIdentifier.builder().name("Sheet1").index(0).length(1).build(), detected.getIdentifier());
        Assertions.assertNull(detected.getRange());
    }

    @Test
    public void testTwoSheetsEmpty() throws Exception {
        var source = getSource("excel", "two-sheets-empty.xlsx").asTable();
        var lines = source.readAll();

        Assertions.assertEquals(lines.size(), 0);

        ExcelSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(ExcelHeaderState.INCLUDED, detected.getHeaderState());
        Assertions.assertEquals(ExcelSheetIdentifier.builder().name("sheet 1").index(0).length(2).build(), detected.getIdentifier());
        Assertions.assertNull(detected.getRange());
    }

    @Test
    public void testFinancialSample() throws Exception {
        var source = getSource("excel", "Financial Sample.xlsx").asTable();
        var lines = source.readAll();

        Assertions.assertEquals(700, lines.size());
        Assertions.assertEquals(
                TupleNode.builder()
                        .add("Segment", ValueNode.ofText("Government"))
                        .add("Country", ValueNode.ofText("Canada"))
                        .add("Product", ValueNode.ofText("Carretera"))
                        .add("Discount Band", ValueNode.ofText("None"))
                        .add("Units Sold", ValueNode.ofCurrency("$   1,618.50", "1618.5", Currency.getInstance("USD")))
                        .add("Manufacturing Price", ValueNode.ofCurrency("$   3.00", "3", Currency.getInstance("USD")))
                        .add("Sale Price", ValueNode.ofCurrency("$   20.00", "20", Currency.getInstance("USD")))
                        .add("Gross Sales", ValueNode.ofCurrency("$   32,370.00", "32370", Currency.getInstance("USD")))
                        .add("Discounts", ValueNode.ofCurrency("$   - 0", "-0", Currency.getInstance("USD")))
                        .add("Sales", ValueNode.ofCurrency("$   32,370.00", "32370", Currency.getInstance("USD")))
                        .add("COGS", ValueNode.ofCurrency("$   16,185.00", "16185", Currency.getInstance("USD")))
                        .add("Profit", ValueNode.ofCurrency("$   16,185.00", "16185", Currency.getInstance("USD")))
                        .add(
                                "Date",
                                ValueNode.ofDate(
                                        "1/1/2014",
                                        new GregorianCalendar(2014, Calendar.JANUARY, 1)
                                                .getTime()
                                                .toInstant()))
                        .add("Month Number", ValueNode.ofInteger("1", "1"))
                        .add("Month Name", ValueNode.ofText("January"))
                        .add("Year", ValueNode.ofText("2014"))
                        .build(),
                lines.at(0));

        ExcelSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(ExcelHeaderState.INCLUDED, detected.getHeaderState());
        Assertions.assertEquals(ExcelSheetIdentifier.builder().name("Sheet1").index(0).length(2).build(), detected.getIdentifier());
        Assertions.assertEquals(
                new ExcelRange(ExcelCellLocation.parse("A1"), ExcelCellLocation.parse("P701")), detected.getRange());
    }

    @Test
    public void testFinancialSampleRoundabout() throws Exception {
        var source = getSource("excel", "Financial Sample.xlsx").asTable();

        var targetFile = Files.createTempFile(null, ".xlsx").toString();
        var target =
                getSource("excel", new FileStore(new LocalStore(), targetFile)).asTable();

        source.forwardTo(target);
        var lines = target.readAll();
        Assertions.assertEquals(700, lines.size());
    }

    @Test
    public void testImages() throws Exception {
        var source = getSource("excel", "images.xlsx").asTable();
        var lines = source.readAll();

        Assertions.assertEquals(19, lines.size());

        ExcelSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(ExcelHeaderState.INCLUDED, detected.getHeaderState());
    }
}
