package io.xpipe.core.data;

public interface DataStructureNodeAcceptor<T extends DataStructureNode> {

    boolean accept(T node) throws Exception;
}
