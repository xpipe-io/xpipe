package io.xpipe.ext.jdbc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.source.WriteMode;

public class JdbcWriteModes {

    public static final Update UPDATE = new Update();
    public static final Insert INSERT = new Insert();
    public static final Merge MERGE = new Merge();

    @JsonTypeName("jdbc.update")
    public static final class Update extends WriteMode {}

    @JsonTypeName("jdbc.insert")
    public static final class Insert extends WriteMode {}

    @JsonTypeName("jdbc.merge")
    public static final class Merge extends WriteMode {}
}
