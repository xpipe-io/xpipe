package io.xpipe.core.source;

import io.xpipe.core.data.type.TupleType;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

@Getter
public class TableMapping {

    private static Integer[] range(int size) {
        var array = new Integer[size];
        for (int i = 0; i < size; i++) {
            array[i] = i;
        }
        return array;
    }

    public static TableMapping empty(TupleType inputType) {
        return new TableMapping(inputType, TupleType.empty(), new Integer[inputType.getSize()]);
    }

    public static TableMapping createIdentity(TupleType inputType) {
        return new TableMapping(inputType, inputType, range(inputType.getSize()));
    }

    public static Optional<TableMapping> createBasic(TupleType inputType, TupleType outputType) {
        // Name based mapping
        if (inputType.hasAllNames()) {
            var array = new Integer[inputType.getSize()];
            for (int i = 0; i < inputType.getNames().size(); i++) {
                var map = mapColumnName(inputType.getNames().get(i), outputType.getNames());
                array[i] = map.isPresent() ? map.getAsInt() : null;
            }
            return Optional.of(new TableMapping(inputType, outputType, array));
        }

        // Index based mapping
        if ((!inputType.hasAllNames() || outputType.hasAllNames()) && inputType.getSize() == outputType.getSize()) {
            return Optional.of(new TableMapping(inputType, outputType, range(inputType.getSize())));
        }

        return Optional.empty();
    }

    private static OptionalInt mapColumnName(String inputName, List<String> outputNames) {
        for (int i = 0; i < outputNames.size(); i++) {
            if (outputNames.get(i) != null && outputNames.get(i).trim().equalsIgnoreCase(inputName.trim())) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    private final TupleType inputType;

    private final TupleType outputType;

    protected final Integer[] columMap;

    public TableMapping(TupleType inputType, TupleType outputType, Integer[] columMap) {
        this.inputType = inputType;
        this.outputType = outputType;
        this.columMap = columMap;
    }

    public boolean isIdentity() {
        return inputType.equals(outputType)
                && Arrays.equals(columMap, range(getInputType().getSize()));
    }

    public boolean isComplete() {
        return IntStream.range(0, outputType.getSize())
                .allMatch(value -> inverseMap(value).isPresent());
    }

    public boolean isComplete(List<String> outputNames) {
        return IntStream.range(0, outputType.getSize())
                .filter(i -> outputNames.contains(outputType.getNames().get(i)))
                .allMatch(value -> inverseMap(value).isPresent());
    }

    public TableMapping sub(List<String> outputNames) {
        var array = Arrays.copyOf(columMap, columMap.length);
        for (int i = 0; i < inputType.getSize(); i++) {
            var mapped = map(i);
            if (mapped.isPresent()
                    && !outputNames.contains(outputType.getNames().get(mapped.getAsInt()))) {
                array[i] = null;
            }
        }
        return new TableMapping(inputType, outputType, array);
    }

    public OptionalInt inverseMap(int outputColumn) {
        for (int i = 0; i < inputType.getNames().size(); i++) {
            if (map(i).orElse(-1) == outputColumn) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    public OptionalInt map(int inputColumn) {
        return columMap[inputColumn] != null ? OptionalInt.of(columMap[inputColumn]) : OptionalInt.empty();
    }
}
