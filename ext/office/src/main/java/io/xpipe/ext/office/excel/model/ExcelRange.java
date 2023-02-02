package io.xpipe.ext.office.excel.model;

import lombok.Value;

@Value
public class ExcelRange {

    ExcelCellLocation begin;
    ExcelCellLocation end;

    public static ExcelRange parse(String s) {
        if (s.contains(":")) {
            var b = ExcelCellLocation.parse(s.split(":")[0]);
            var e = ExcelCellLocation.parse(s.split(":")[1]);
            return new ExcelRange(b, e);
        }

        throw new IllegalArgumentException("Invalid excel range: " + s);
    }

    public String toString() {
        return begin.toString() + ":" + end.toString();
    }
}
