package io.xpipe.ext.office.excel;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.impl.SimpleTableWriteConnection;
import io.xpipe.core.impl.StreamWriteConnection;
import io.xpipe.core.source.TableMapping;
import io.xpipe.ext.office.excel.model.ExcelHeaderState;
import lombok.Getter;
import org.apache.poi.EmptyFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.time.Instant;
import java.util.Date;
import java.util.List;

public class ExcelWriteConnection extends StreamWriteConnection implements SimpleTableWriteConnection<ExcelSource> {

    @Getter
    private final ExcelSource source;

    private Workbook workbook;
    private Sheet sheet;

    private int counter;
    private boolean headerWritten;

    public ExcelWriteConnection(ExcelSource source) {
        super(source.getStore(), null);
        this.source = source;
    }

    @Override
    public void init() throws Exception {
        super.init();
        try {
            workbook = source.getStore().canOpen()
                    ? WorkbookFactory.create(source.getStore().openBufferedInput())
                    : new XSSFWorkbook();
        } catch (EmptyFileException ex) {
            workbook = new XSSFWorkbook();
        }

        var sheets = ExcelHelper.getSheets(workbook);
        if (sheets.size() == 0) {
            sheets = List.of(workbook.createSheet(source.getIdentifier().getName()));
        }

        sheet = sheets.stream()
                .filter(s -> s.getSheetName().equals(source.getIdentifier().getName()))
                .findFirst()
                .orElse(workbook.getSheetAt(source.getIdentifier().getIndex()));
    }

    @Override
    public void close() throws Exception {
        workbook.write(outputStream);
        workbook.close();
        super.close();
    }

    private void writeHeader(TableMapping mapping) {
        if (!headerWritten && source.getHeaderState() == ExcelHeaderState.INCLUDED) {
            var row = sheet.createRow(counter++);
            for (int i = 0; i < mapping.getOutputType().getSize(); i++) {
                var offset =
                        source.getRange() != null ? source.getRange().getBegin().getColumn() - 1 + i : i;
                var cell = row.createCell(offset);
                cell.setCellValue(mapping.getOutputType().getNames().get(i));
            }
            headerWritten = true;
        }
    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor(TableMapping mapping) {
        writeHeader(mapping);
        return node -> {
            var row = sheet.createRow(counter);
            for (int i = 0; i < mapping.getOutputType().getSize(); i++) {
                var offset =
                        source.getRange() != null ? source.getRange().getBegin().getColumn() - 1 + i : i;
                var cell = row.createCell(offset);
                writeValue(cell, node.at(mapping.inverseMap(i).orElseThrow()).asValue());
            }
            counter++;

            return true;
        };
    }

    private void writeValue(Cell cell, ValueNode node) {
        if (node.hasMetaAttribute(DataStructureNode.IS_BOOLEAN)) {
            cell.setCellValue(node.hasMetaAttribute(DataStructureNode.BOOLEAN_TRUE));
        } else if (node.hasMetaAttribute(DataStructureNode.IS_DATE)) {
            cell.setCellValue(Date.from(Instant.parse(node.getMetaAttribute(DataStructureNode.DATE_VALUE))));

            var styleDateFormat = workbook.createCellStyle();
            styleDateFormat.setDataFormat((short) 0xe);
            cell.setCellStyle(styleDateFormat);
        } else if (node.hasMetaAttribute(DataStructureNode.IS_CURRENCY)) {
            cell.setCellValue(Double.parseDouble(node.getMetaAttribute(DataStructureNode.DECIMAL_VALUE)));

            var styleCurrencyFormat = workbook.createCellStyle();
            styleCurrencyFormat.setDataFormat((short) 0x7);
            cell.setCellStyle(styleCurrencyFormat);
        } else if (node.hasMetaAttribute(DataStructureNode.IS_INTEGER)) {
            cell.setCellValue(Double.parseDouble(node.getMetaAttribute(DataStructureNode.INTEGER_VALUE)));
        } else if (node.hasMetaAttribute(DataStructureNode.IS_DECIMAL)) {
            cell.setCellValue(Double.parseDouble(node.getMetaAttribute(DataStructureNode.DECIMAL_VALUE)));
        } else {
            cell.setCellValue(node.asString());
        }
    }
}
