package org.neo4j.rest.graphdb.transaction;

/**
* @author mh
* @since 20.05.15
*/
public interface TransactionFinishListener {
    void comitted();
    void rolledBack();
}
