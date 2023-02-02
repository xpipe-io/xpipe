package io.xpipe.ext.office.excel;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import io.xpipe.core.impl.StreamReadConnection;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.ext.office.excel.model.ExcelHeaderState;
import io.xpipe.extension.util.DataTypeParser;
import org.apache.poi.EmptyFileException;
import org.apache.poi.ss.usermodel.*;

import java.util.Collections;
import java.util.List;

public class ExcelReadConnection extends StreamReadConnection implements TableReadConnection {

    private final ExcelSource source;
    private Workbook workbook;
    private Sheet sheet;
    private TupleType type;

    public ExcelReadConnection(ExcelSource source) {
        super(source.getStore(), null);
        this.source = source;
    }

    @Override
    public void init() throws Exception {
        super.init();
        try {
            workbook = WorkbookFactory.create(inputStream);
        } catch (EmptyFileException ex) {
            return;
        }
        var sheets = ExcelHelper.getSheets(workbook);
        sheet = sheets.stream()
                .filter(s -> s.getSheetName().equals(source.getIdentifier().getName()))
                .findFirst()
                .orElse(workbook.getSheetAt(source.getIdentifier().getIndex()));

        if (source.getHeaderState() == ExcelHeaderState.INCLUDED) {
            var names = ExcelHelper.rowStream(sheet, source.getRange(), false)
                    .findFirst()
                    .map(cells -> cells.stream()
                            .map(cell -> map(cell).asString().trim())
                            .toList())
                    .orElse(List.of());
            type = TupleType.of(names, Collections.nCopies(names.size(), ValueType.of()));
        } else {
            type = TupleType.of(Collections.nCopies(
                    source.getRange().getEnd().getColumn()
                            - source.getRange().getBegin().getColumn()
                            + 1,
                    ValueType.of()));
        }
    }

    @Override
    public void close() throws Exception {
        if (workbook != null) {
            workbook.close();
        }
        super.close();
    }

    @Override
    public TupleType getDataType() {
        return type;
    }

    @Override
    public void withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
        if (workbook == null) {
            return;
        }

        var iterator = ExcelHelper.rowStream(sheet, source.getRange(), source.isContinueSelection())
                .skip(source.getHeaderState() == ExcelHeaderState.INCLUDED ? 1 : 0)
                .iterator();
        while (iterator.hasNext()) {
            var row = iterator.next();
            var t = row.stream().map(cell -> map(cell)).limit(type.getSize()).toList();
            var tuple = TupleNode.of(type.getNames(), t);
            if (!lineAcceptor.accept(tuple)) {
                break;
            }
            ;
        }
    }

    private ValueNode map(Cell cell) {
        DataFormatter dataFormatter = new DataFormatter();
        dataFormatter.setUse4DigitYearsInAllDateFormats(true);
        String rawValue = dataFormatter.formatCellValue(cell);
        return switch (cell.getCellType()) {
            case _NONE -> ValueNode.nullValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    var date = cell.getDateCellValue();
                    var instant = date.toInstant();
                    yield ValueNode.ofDate(rawValue, instant);
                }

                var monetary = DataTypeParser.parseMonetary(rawValue);
                if (monetary.isPresent()) {
                    yield monetary.get();
                }

                var number = DataTypeParser.parseNumber(rawValue);
                if (number.isPresent()) {
                    yield number.get();
                }

                yield ValueNode.ofDecimal(rawValue, cell.getNumericCellValue());
            }
            case STRING -> {
                yield ValueNode.ofText(cell.getStringCellValue());
            }
            case FORMULA -> {
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                evaluator.evaluateInCell(cell);
                yield map(cell);
            }
            case BLANK -> {
                yield ValueNode.nullValue();
            }
            case BOOLEAN -> {
                yield ValueNode.ofBoolean(cell.getBooleanCellValue());
            }
            case ERROR -> {
                yield ValueNode.nullValue();
            }
        };
    }
}
