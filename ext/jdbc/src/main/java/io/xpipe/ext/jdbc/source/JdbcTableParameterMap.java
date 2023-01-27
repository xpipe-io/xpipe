package io.xpipe.ext.jdbc.source;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import io.xpipe.core.source.TableMapping;
import io.xpipe.core.source.WriteMode;
import io.xpipe.ext.jdbc.JdbcDataTypeCategory;
import io.xpipe.ext.jdbc.JdbcDialect;
import io.xpipe.ext.jdbc.JdbcHelper;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
@JsonSerialize(as = TableMapping.class)
public class JdbcTableParameterMap extends TableMapping {

    ColumnInformation information;
    List<String> insertTableColumns;
    boolean canPerformUpdates;
    boolean requiresMerge;
    boolean clearOriginalTable;
    public JdbcTableParameterMap(
            TupleType inputType,
            TupleType outputType,
            Integer[] columMap,
            ColumnInformation information,
            List<String> insertTableColumns,
            boolean canPerformUpdates,
            boolean requiresMerge,
            boolean clearOriginalTable) {
        super(inputType, outputType, columMap);
        this.information = information;
        this.insertTableColumns = insertTableColumns;
        this.canPerformUpdates = canPerformUpdates;
        this.requiresMerge = requiresMerge;
        this.clearOriginalTable = clearOriginalTable;
    }

    public static Optional<JdbcTableParameterMap> create(
            Connection connection, String table, TupleType inputType, WriteMode mode) throws SQLException {
        var information = ColumnInformation.create(connection, table);
        var dataType = TupleType.of(
                information.getAllTableColumns(),
                Collections.nCopies(information.getAllTableColumns().size(), ValueType.of()));
        var basicMap = TableMapping.createBasic(inputType, dataType);
        if (basicMap.isEmpty()) {
            return Optional.empty();
        }

        var canPerformUpdates = false;

        var baseInsertColumns = new ArrayList<>(information.getAllTableColumns());
        baseInsertColumns.removeIf(s -> information.getGeneratedColumns().contains(s));

        var requiresMerge =
                mode.equals(WriteMode.REPLACE) && information.getIdentifiers().size() > 0;
        List<String> insertColumns = requiresMerge ? information.getTemporaryTableColumns() : baseInsertColumns;

        var clearOriginalTable =
                mode.equals(WriteMode.REPLACE) && information.getIdentifiers().size() == 0;

        TableMapping map = null;
        var temporaryMap = basicMap.get().sub(information.getTemporaryTableColumns());
        if (temporaryMap.isComplete(information.getTemporaryTableColumns())) {
            map = temporaryMap;
            canPerformUpdates = true;
        }

        var updateMap = basicMap.get().sub(information.getUpdateTableColumns());
        if (map == null && updateMap.isComplete(information.getUpdateTableColumns())) {
            map = updateMap;
        }

        if (map == null) {
            return Optional.empty();
        }

        if (requiresMerge && !canPerformUpdates) {
            return Optional.empty();
        }

        if (information.getIdentifiers().size() == 0) {
            canPerformUpdates = false;
        }

        return Optional.of(new JdbcTableParameterMap(
                inputType,
                dataType,
                map.getColumMap(),
                information,
                insertColumns,
                canPerformUpdates,
                requiresMerge,
                clearOriginalTable));
    }

    public int getTupleIndexOfColumnName(String name) {
        return inverseMap(getOutputType().getNames().indexOf(name)).orElseThrow();
    }

    public boolean isCanPerformUpdates() {
        return canPerformUpdates;
    }

    public boolean hasIdentifiers() {
        return information.getIdentifiers().size() > 0;
    }

    @Value
    public static class ColumnInformation {
        List<String> allTableColumns;
        List<String> temporaryTableColumns;
        List<String> updateTableColumns;
        List<String> generatedColumns;
        List<String> identifiers;

        List<JdbcDataTypeCategory> jdbcCategories;
        List<Integer> jdbcDataTypes;

        public static ColumnInformation create(Connection connection, String table) throws SQLException {
            var type = JdbcDialect.getDialect(connection);
            var columns = JdbcHelper.getTableColumnNames(connection, table);

            var identifiers = JdbcHelper.getPrimaryKeys(connection, table);

            var resultSetStatement = connection.createStatement();
            ResultSet resultSet = resultSetStatement.executeQuery(type.createQueryAllSql(table));

            var generated = new ArrayList<>(JdbcHelper.getGeneratedColumns(connection, table, resultSet));
            generated.addAll(type.determineAdditionalGeneratedColumns(connection, table, columns));

            var temporaryTableColumns = new ArrayList<>(columns);
            temporaryTableColumns.removeIf(s -> generated.contains(s) && !identifiers.contains(s));

            var updateColumns = new ArrayList<>(columns);
            updateColumns.removeIf(s -> identifiers.contains(s) || generated.contains(s));

            var categories = new ArrayList<JdbcDataTypeCategory>();
            var types = new ArrayList<Integer>();
            for (int i = 0; i < columns.size(); i++) {
                var category = type.getCategory(resultSet, i);
                var jdbcType = resultSet.getMetaData().getColumnType(i + 1);
                categories.add(category);
                types.add(jdbcType);
            }

            resultSetStatement.close();

            return new ColumnInformation(
                    columns, temporaryTableColumns, updateColumns, generated, identifiers, categories, types);
        }
    }
}
