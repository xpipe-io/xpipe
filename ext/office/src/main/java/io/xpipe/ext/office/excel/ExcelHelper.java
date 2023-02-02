package io.xpipe.ext.office.excel;

import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.office.excel.model.ExcelRange;
import io.xpipe.ext.office.excel.model.ExcelSheetIdentifier;
import org.apache.poi.EmptyFileException;
import org.apache.poi.ss.usermodel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ExcelHelper {

    public static Stream<List<Cell>> rowStream(Sheet sheet, ExcelRange range, boolean continuousSelection) {
        return StreamSupport.stream(sheet.spliterator(), false)
                .skip(range != null ? range.getBegin().getRow() - 1 : 0)
                .limit(
                        range == null || continuousSelection
                                ? Integer.MAX_VALUE
                                : range.getEnd().getRow() - range.getBegin().getRow() + 1)
                .map(cells -> StreamSupport.stream(cells.spliterator(), false)
                        .skip(range != null ? range.getBegin().getColumn() - 1 : 0)
                        .toList())
                .takeWhile(cells -> !cells.stream()
                        .allMatch(
                                cell -> cell.getCellType() == CellType._NONE || cell.getCellType() == CellType.BLANK));
    }

    public static List<Sheet> getSheets(Workbook workbook) {
        var sheets = new ArrayList<Sheet>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            sheets.add(workbook.getSheetAt(i));
        }
        return sheets;
    }

    public static ExcelSheetIdentifier getDefaultSelected(
            ExcelSheetIdentifier identifier, List<ExcelSheetIdentifier> available) {
        if (identifier == null) {
            return available.size() > 0 ? available.get(0) : null;
        }

        var byName = available.stream()
                .filter(identifier1 -> identifier1.getName().equals(identifier.getName()))
                .findFirst();
        if (byName.isPresent()) {
            return byName.get();
        }

        return available.size() == identifier.getLength() ? available.get(identifier.getIndex()) : null;
    }

    public static List<ExcelSheetIdentifier> getSheetIdentifiers(Workbook workbook) {
        var sheets = getSheets(workbook);
        return IntStream.range(0, sheets.size())
                .<ExcelSheetIdentifier>mapToObj(operand -> ExcelSheetIdentifier.builder()
                        .name(sheets.get(operand).getSheetName())
                        .index(operand)
                        .length(sheets.size())
                        .build())
                .toList();
    }

    public static List<ExcelSheetIdentifier> getSheetIdentifiers(StreamDataStore store) throws Exception {
        if (!store.canOpen()) {
            return List.of();
        }

        try (Workbook workbook = WorkbookFactory.create(store.openBufferedInput())) {
            return getSheetIdentifiers(workbook);
        } catch (EmptyFileException ex) {
            return List.of();
        }
    }
}
