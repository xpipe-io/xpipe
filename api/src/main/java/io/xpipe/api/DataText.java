package io.xpipe.api;

import io.xpipe.core.source.DataSourceInfo;

import java.util.List;

public interface DataText extends DataSource, Iterable<String> {

    DataSourceInfo.Text getInfo();

    List<String> readAllLines();

    List<String> readLines(int maxLines);

    String readAll();

    String read(int maxCharacters);
}
