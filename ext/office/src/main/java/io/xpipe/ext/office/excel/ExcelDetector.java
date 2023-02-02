package io.xpipe.ext.office.excel;

import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.office.excel.model.ExcelCellLocation;
import io.xpipe.ext.office.excel.model.ExcelHeaderState;
import io.xpipe.ext.office.excel.model.ExcelRange;
import io.xpipe.ext.office.excel.model.ExcelSheetIdentifier;
import org.apache.poi.EmptyFileException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

public class ExcelDetector {

    public static ExcelSource defaultSource(StreamDataStore store) {
        return ExcelSource.builder()
                .store(store)
                .identifier(ExcelSheetIdentifier.builder().name("Sheet1").index(0).length(1).build())
                .headerState(ExcelHeaderState.INCLUDED)
                .continueSelection(true)
                .build();
    }

    public static ExcelSource detect(StreamDataStore store) throws Exception {
        if (!store.canOpen()) {
            return defaultSource(store);
        }

        try (Workbook workbook = WorkbookFactory.create(store.openBufferedInput())) {
            var sheets = ExcelHelper.getSheets(workbook);
            var sheet = sheets.get(0);
            var identifier = ExcelSheetIdentifier.builder().name(sheet.getSheetName()).index(0).length(sheets.size()).build();
            var state = ExcelHeaderState.INCLUDED;
            var continueSelection = true;
            var range = detectRange(sheet);
            return ExcelSource.builder()
                    .store(store)
                    .continueSelection(continueSelection)
                    .identifier(identifier)
                    .headerState(state)
                    .range(range)
                    .build();
        } catch (EmptyFileException ex) {
            return defaultSource(store);
        }
    }

    public static ExcelSource detect(StreamDataStore store, ExcelSheetIdentifier sheetId) throws Exception {
        if (!store.canOpen()) {
            return defaultSource(store);
        }

        try (Workbook workbook = WorkbookFactory.create(store.openBufferedInput())) {
            var sheets = ExcelHelper.getSheets(workbook);
            var sheet = sheets.size() > 0 ? sheets.get(sheetId.getIndex()) : null;
            var identifier = sheet != null ? ExcelSheetIdentifier.builder().name(sheet.getSheetName()).index(0).length(sheets.size()).build() : null;
            var state = ExcelHeaderState.INCLUDED;
            var continueSelection = true;
            var range = sheet != null ? detectRange(sheet) : null;
            return ExcelSource.builder()
                    .store(store)
                    .continueSelection(continueSelection)
                    .identifier(identifier)
                    .headerState(state)
                    .range(range)
                    .build();
        }
    }

    private static ExcelRange detectRange(Sheet sheet) {
        var rowsStart = 1;
        var rowsEnd = sheet.getLastRowNum() + 1;

        var empty = StreamSupport.stream(sheet.spliterator(), false).findAny().isEmpty();
        if (empty) {
            return null;
        }

        for (Row cells : sheet) {
            if (!isRowEmpty(cells) && !hasMergedRegions(sheet, cells)) {
                break;
            }

            rowsStart++;
        }

        AtomicInteger columnStart = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger columnEnd = new AtomicInteger(1);
        StreamSupport.stream(sheet.spliterator(), false).skip(rowsStart - 1).forEach(cells -> {
            var s = getRowStart(cells);
            if (s < columnStart.get()) {
                columnStart.set(s);
            }

            var e = (int) StreamSupport.stream(cells.spliterator(), false).count();
            if (e > columnEnd.get()) {
                columnEnd.set(e);
            }
        });

        return new ExcelRange(
                new ExcelCellLocation(rowsStart, columnStart.get()), new ExcelCellLocation(rowsEnd, columnEnd.get()));
    }

    private static boolean isRowEmpty(Row row) {
        return StreamSupport.stream(row.spliterator(), false)
                .allMatch(cell -> cell.getCellType() == CellType._NONE || cell.getCellType() == CellType.BLANK);
    }

    private static boolean hasMergedRegions(Sheet sheet, Row row) {
        int count = 0;
        for (int i = 0; i < sheet.getNumMergedRegions(); ++i) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            if (range.getFirstRow() <= row.getRowNum() && range.getLastRow() >= row.getRowNum()) ++count;
        }
        return count > 0;
    }

    private static int getRowStart(Row row) {
        var index = 1;
        for (Cell cell : row) {
            if (cell.getCellType() != CellType._NONE && cell.getCellType() != CellType.BLANK) {
                break;
            }

            index++;
        }
        return index;
    }
}
