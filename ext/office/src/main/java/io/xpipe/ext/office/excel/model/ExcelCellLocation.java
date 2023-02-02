package io.xpipe.ext.office.excel.model;

import lombok.Value;
import org.apache.poi.ss.util.CellReference;

import java.util.regex.Pattern;

@Value
public class ExcelCellLocation {

    private static final Pattern ID_PATTERN = Pattern.compile("([a-zA-Z]+)(\\d+)");
    int row;
    int column;

    public static ExcelCellLocation parse(String id) {
        var m = ID_PATTERN.matcher(id);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid cell id: " + id);
        }

        var column = toColumnIndex(m.group(1));
        var row = Integer.parseInt(m.group(2));
        return new ExcelCellLocation(row, column);
    }

    private static String fromColumnIndex(int index) {
        return CellReference.convertNumToColString(index - 1);
    }

    private static int toColumnIndex(String id) {
        return CellReference.convertColStringToIndex(id) + 1;
    }

    public String toString() {
        return fromColumnIndex(getColumn()) + getRow();
    }
}
