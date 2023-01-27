package io.xpipe.ext.jdbc;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.extension.util.TypeConverter;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.sql.Types.*;

public abstract class JdbcDataTypeCategory {

    public static JdbcDataTypeCategory INTEGER_CATEGORY = new ValueCategory(true, TINYINT, SMALLINT, INTEGER, BIGINT) {

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            var decimal = result.getBigDecimal(index);
            var integer = decimal != null ? decimal.toBigInteger() : null;
            return ValueNode.ofInteger(integer);
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                throws SQLException {
            var integer = TypeConverter.parseInteger(value);
            statement.setBigDecimal(index, integer != null ? new BigDecimal(integer) : null);
        }
    };

    public static JdbcDataTypeCategory DECIMAL_CATEGORY =
            new ValueCategory(true, NUMERIC, FLOAT, DOUBLE, REAL, DECIMAL) {

                @Override
                public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
                    var decimal = result.getBigDecimal(index);
                    return ValueNode.ofDecimal(decimal);
                }

                @Override
                public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                        throws SQLException {
                    var decimal = TypeConverter.parseDecimal(value);
                    statement.setBigDecimal(index, decimal);
                }
            };

    public static JdbcDataTypeCategory STRING_CATEGORY =
            new ValueCategory(true, CHAR, VARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR, LONGVARCHAR) {

                @Override
                public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
                    var string = result.getString(index);
                    return ValueNode.of(string);
                }

                @Override
                public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                        throws SQLException {
                    var string = value.asString();
                    statement.setString(index, string);
                }
            };

    public static JdbcDataTypeCategory BLOB_CATEGORY = new ValueCategory(true, BLOB) {

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            var string = result.getString(index);
            return ValueNode.of(string);
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                throws SQLException {
            var string = value.asString();
            statement.setString(index, string);
        }
    };

    public static JdbcDataTypeCategory BINARY_CATEGORY = new ValueCategory(true, BINARY, LONGVARBINARY, VARBINARY) {

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            var bytes = result.getBytes(index);
            return ValueNode.ofBytes(bytes);
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                throws SQLException {
            var bytes = value.getRawData();
            statement.setBytes(index, bytes);
        }
    };

    public static JdbcDataTypeCategory BOOLEAN_CATEGORY = new ValueCategory(true, BIT) {

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            Boolean bool = result.getBoolean(index);
            if (result.wasNull()) {
                bool = null;
            }
            return ValueNode.ofBoolean(bool);
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                throws SQLException {
            var bool = TypeConverter.parseBoolean(value);
            statement.setBoolean(index, bool);
        }
    };

    public static JdbcDataTypeCategory TIMESTAMP_CATEGORY =
            new ValueCategory(true, TIMESTAMP, TIMESTAMP_WITH_TIMEZONE) {

                @Override
                public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
                    var timestamp = result.getTimestamp(index);
                    return ValueNode.of(timestamp);
                }

                @Override
                public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                        throws SQLException {
                    var timestamp = Timestamp.valueOf(value.asString());
                    statement.setTimestamp(index, timestamp);
                }
            };

    public static JdbcDataTypeCategory DATE_CATEGORY = new ValueCategory(true, DATE) {

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            var date = result.getDate(index);
            return ValueNode.of(date);
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                throws SQLException {
            var date = Date.valueOf(value.asString());
            statement.setDate(index, date);
        }
    };

    public static JdbcDataTypeCategory TIME_CATEGORY = new ValueCategory(true, TIME) {

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            var date = result.getTime(index);
            return ValueNode.of(date);
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                throws SQLException {
            var date = Time.valueOf(value.asString());
            statement.setTime(index, date);
        }
    };

    public static JdbcDataTypeCategory XML_CATEGORY = new ValueCategory(true, SQLXML) {

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            var xml = result.getSQLXML(index);
            var string = xml != null ? xml.getString() : null;
            return ValueNode.of(string);
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                throws SQLException {
            var xml = statement.getConnection().createSQLXML();
            xml.setString(value.asString());
            statement.setSQLXML(index, xml);
        }
    };

    public static JdbcDataTypeCategory UUID_CATEGORY = new JdbcDataTypeCategory() {

        @Override
        public boolean isApplicable(ResultSet result, int index) throws SQLException {
            return UUID.class.getName().equals(result.getMetaData().getColumnClassName(index));
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, DataStructureNode value)
                throws SQLException {
            var id = UUID.fromString(value.asString());
            statement.setObject(index, id);
        }

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            var object = result.getObject(index, UUID.class);
            return ValueNode.of(object);
        }
    };

    public static JdbcDataTypeCategory OTHER_CATEGORY = new SimpleCategory(OTHER) {

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, DataStructureNode value)
                throws SQLException {
            statement.setString(index, value.asString());
        }

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            var object = result.getObject(index);
            var string = object.toString();
            return ValueNode.of(string);
        }
    };

    public static JdbcDataTypeCategory CATCH_ALL_CATEGORY = new JdbcDataTypeCategory() {

        @Override
        public boolean isApplicable(ResultSet result, int index) throws SQLException {
            return true;
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, DataStructureNode value)
                throws SQLException {
            var string = value.asString();
            statement.setString(index, string);
        }

        @Override
        public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
            var string = result.getString(index);
            return ValueNode.of(string);
        }
    };
    public static List<JdbcDataTypeCategory> DEFAULT_CATEGORIES = List.of(
            INTEGER_CATEGORY,
            DECIMAL_CATEGORY,
            STRING_CATEGORY,
            BLOB_CATEGORY,
            BINARY_CATEGORY,
            BOOLEAN_CATEGORY,
            TIMESTAMP_CATEGORY,
            DATE_CATEGORY,
            TIME_CATEGORY,
            XML_CATEGORY,
            UUID_CATEGORY,
            OTHER_CATEGORY,
            CATCH_ALL_CATEGORY);

    public static JdbcDataTypeCategory getApplicableCategory(JdbcDialect type, ResultSet result, int index)
            throws SQLException {
        var all = new ArrayList<>(DEFAULT_CATEGORIES);
        all.addAll(0, type.getAdditionalCategories());
        for (JdbcDataTypeCategory jdbcDataTypeCategory : all) {
            if (jdbcDataTypeCategory.isApplicable(result, index + 1)) {
                return jdbcDataTypeCategory;
            }
        }
        throw new AssertionError();
    }

    public abstract boolean isApplicable(ResultSet result, int index) throws SQLException;

    public abstract void setValue(PreparedStatement statement, int jdbcDataType, int index, DataStructureNode value)
            throws SQLException;

    public abstract DataStructureNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException;

    public abstract static class ValueCategory extends SimpleCategory {

        private final boolean setNullAutomatically;

        public ValueCategory(boolean setNullAutomatically, Integer... jdbcTypeIds) {
            super(jdbcTypeIds);
            this.setNullAutomatically = setNullAutomatically;
        }

        @Override
        public void setValue(PreparedStatement statement, int jdbcDataType, int index, DataStructureNode value)
                throws SQLException {
            if (!value.isValue()) {
                throw new IllegalArgumentException("Not a value");
            }

            if (setNullAutomatically && value.hasMetaAttribute(DataStructureNode.IS_NULL)) {
                statement.setNull(index, jdbcDataType);
                return;
            }

            setValue(statement, jdbcDataType, index, value.asValue());
        }

        @Override
        public abstract ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException;

        public abstract void setValue(PreparedStatement statement, int jdbcDataType, int index, ValueNode value)
                throws SQLException;
    }

    public abstract static class UserDefinedCategory extends SimpleCategory {

        public UserDefinedCategory(int jdbcTypeIds) {
            super(jdbcTypeIds);
        }

        @Override
        public final void setValue(PreparedStatement statement, int jdbcDataType, int index, DataStructureNode value)
                throws SQLException {
            if (value.hasMetaAttribute(DataStructureNode.IS_NULL)) {
                statement.setNull(index, NULL);
                return;
            }

            setValueNonNull(statement, jdbcDataType, index, value.asValue());
        }

        @Override
        public abstract ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException;

        public abstract void setValueNonNull(
                PreparedStatement statement, int jdbcDataType, int index, DataStructureNode value) throws SQLException;
    }

    public abstract static class SimpleCategory extends JdbcDataTypeCategory {

        private final List<Integer> jdbcTypeIds;

        public SimpleCategory(Integer... jdbcTypeIds) {
            this.jdbcTypeIds = List.of(jdbcTypeIds);
        }

        @Override
        public boolean isApplicable(ResultSet result, int index) throws SQLException {
            var jdbcDataType = result.getMetaData().getColumnType(index);
            return jdbcTypeIds.contains(jdbcDataType);
        }
    }
}
