package io.xpipe.core.data.node;

public interface DataStructureNodeAcceptor<T extends DataStructureNode> {

    boolean accept(T node) throws Exception;
}
