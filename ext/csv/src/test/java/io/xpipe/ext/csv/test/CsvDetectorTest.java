package io.xpipe.ext.csv.test;

import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.ext.csv.CsvDelimiter;
import io.xpipe.ext.csv.CsvHeaderState;
import io.xpipe.ext.csv.CsvSource;
import io.xpipe.extension.util.DaemonExtensionTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CsvDetectorTest extends DaemonExtensionTest {

    @Test
    public void testAirtravel() throws Exception {
        var source = getSource("csv", "airtravel.csv").asTable();
        var lines = source.readAll();
        var names = lines.at(0).getKeyNames();
        Assertions.assertEquals(lines.size(), 12);
        Assertions.assertEquals(
                lines.at(0),
                TupleNode.of(
                        names,
                        List.of(
                                ValueNode.of("JAN"),
                                ValueNode.of(340),
                                ValueNode.of(360),
                                ValueNode.of(417))));
        Assertions.assertEquals(
                lines.at(lines.size() - 1),
                TupleNode.of(
                        names,
                        List.of(ValueNode.of("DEC"), ValueNode.of(337), ValueNode.of(405), ValueNode.of(432))));

        CsvSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(detected.getHeaderState(), CsvHeaderState.INCLUDED);
        Assertions.assertEquals(detected.getDelimiter(), ',');
    }

    @Test
    public void testPasswordRecovery() throws Exception {
        var source = getSource("csv", "username-password-recovery-code.csv").asTable();
        var lines = source.readAll();
        var names = lines.at(0).getKeyNames();
        Assertions.assertEquals(
                names,
                List.of(
                        "Username",
                        "Identifier",
                        "One-time password",
                        "Recovery code",
                        "First name",
                        "Last name",
                        "Department",
                        "Location"));
        Assertions.assertEquals(lines.size(), 5);
        Assertions.assertEquals(
                lines.at(0),
                TupleNode.of(
                        names,
                        List.of(
                                ValueNode.of("booker12"),
                                ValueNode.of("9012"),
                                ValueNode.of("12se74"),
                                ValueNode.of("rb9012"),
                                ValueNode.of("Rachel"),
                                ValueNode.of("Booker"),
                                ValueNode.of("Sales"),
                                ValueNode.of("Manchester"))));
        Assertions.assertEquals(
                lines.at(lines.size() - 1),
                TupleNode.of(
                        names,
                        List.of(
                                ValueNode.of("smith79"),
                                ValueNode.of("5079"),
                                ValueNode.of("09ja61"),
                                ValueNode.of("js5079"),
                                ValueNode.of("Jamie"),
                                ValueNode.of("Smith"),
                                ValueNode.of("Engineering"),
                                ValueNode.of("Manchester"))));

        CsvSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(detected.getHeaderState(), CsvHeaderState.INCLUDED);
        Assertions.assertEquals(detected.getDelimiter(), ';');
    }

    @Test
    public void testHeightWeight() throws Exception {
        var source = getSource("csv", "hw_25000.csv").asTable();
        var lines = source.readAll();
        var names = lines.at(0).getKeyNames();

        Assertions.assertEquals(names, List.of("Index", "Height(Inches)", "Weight(Pounds)"));
        Assertions.assertEquals(lines.size(), 25000);
        Assertions.assertEquals(
                lines.at(0),
                TupleNode.of(names, List.of(ValueNode.of("1"), ValueNode.of("65.78331"), ValueNode.of("112.9925"))));
        Assertions.assertEquals(
                lines.at(lines.size() - 1),
                TupleNode.of(
                        names, List.of(ValueNode.of("25000"), ValueNode.of("68.87761"), ValueNode.of("124.8742"))));

        CsvSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(detected.getHeaderState(), CsvHeaderState.INCLUDED);
        Assertions.assertEquals(detected.getDelimiter(), ',');
    }

    @Test
    public void testEnterpriseSurvey() throws Exception {
        var source = getSource("csv", "annual-enterprise-survey-2020-financial-year-provisional-csv.csv")
                .asTable();
        var lines = source.readAll();
        var names = lines.at(0).getKeyNames();

        Assertions.assertEquals(
                names,
                List.of(
                        "Year",
                        "Industry_aggregation_NZSIOC",
                        "Industry_code_NZSIOC",
                        "Industry_name_NZSIOC",
                        "Units",
                        "Variable_code",
                        "Variable_name",
                        "Variable_category",
                        "Value",
                        "Industry_code_ANZSIC06"));
        Assertions.assertEquals(lines.size(), 37080);
        Assertions.assertEquals(
                lines.at(0),
                TupleNode.of(
                        names,
                        List.of(
                                ValueNode.of("2020"),
                                ValueNode.of("Level 1"),
                                ValueNode.of("99999"),
                                ValueNode.of("All industries"),
                                ValueNode.of("Dollars (millions)"),
                                ValueNode.of("H01"),
                                ValueNode.of("Total income"),
                                ValueNode.of("Financial performance"),
                                ValueNode.of("733,258"),
                                ValueNode.of(
                                        "ANZSIC06 divisions A-S (excluding classes K6330, L6711, O7552, O760, O771, O772, S9540, S9601, S9602, and S9603)"))));
        Assertions.assertEquals(
                lines.at(lines.size() - 1),
                TupleNode.of(
                        names,
                        List.of(
                                ValueNode.of("2013"),
                                ValueNode.of("Level 3"),
                                ValueNode.of("ZZ11"),
                                ValueNode.of("Food product manufacturing"),
                                ValueNode.of("Percentage"),
                                ValueNode.of("H41"),
                                ValueNode.of("Liabilities structure"),
                                ValueNode.of("Financial ratios"),
                                ValueNode.of("46"),
                                ValueNode.of(
                                        "ANZSIC06 groups C111, C112, C113, C114, C115, C116, C117, C118, and C119"))));

        CsvSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(detected.getHeaderState(), CsvHeaderState.INCLUDED);
        Assertions.assertEquals(detected.getDelimiter(), ',');
    }

    @Test
    public void testJobStatus() throws Exception {
        var source = getSource("csv", "job_status.csv").asTable();
        var lines = source.readAll();
        var names = lines.at(0).getKeyNames();

        names.forEach(Assertions::assertNull);
        Assertions.assertEquals(lines.size(), 9);
        Assertions.assertEquals(lines.at(0), TupleNode.of(names, List.of(ValueNode.of("Job Status"))));
        Assertions.assertEquals(lines.at(lines.size() - 1), TupleNode.of(names, List.of(ValueNode.of("Seasonal"))));

        CsvSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(detected.getHeaderState(), CsvHeaderState.OMITTED);
        Assertions.assertEquals(CsvDelimiter.getDefault().getNamedCharacter().getCharacter(), detected.getDelimiter());
    }

    @Test
    public void testEmpty() throws Exception {
        var source = getSource("csv", "empty.csv").asTable();
        var lines = source.readAll();

        Assertions.assertEquals(lines.size(), 0);

        CsvSource detected = source.getInternalSource().asNeeded();
        Assertions.assertEquals(detected.getHeaderState(), CsvHeaderState.INCLUDED);
        Assertions.assertEquals(detected.getDelimiter(), ',');
    }
}
