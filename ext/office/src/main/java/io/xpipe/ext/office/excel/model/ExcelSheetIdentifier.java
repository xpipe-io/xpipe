package io.xpipe.ext.office.excel.model;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@SuperBuilder
public final class ExcelSheetIdentifier {

    private final String name;
    private final int index;
    private final int length;
}
