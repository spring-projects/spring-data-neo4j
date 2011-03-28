package org.neo4j.rest.graphdb;

import org.neo4j.graphdb.traversal.TraversalDescription;

/**
 * @author Michael Hunger
 * @since 03.02.11
 */
public interface RestTraversalDescription extends TraversalDescription {
    TraversalDescription prune(ScriptLanguage language, String code);

    TraversalDescription filter(ScriptLanguage language, String code);

    TraversalDescription maxDepth(int depth);

    public enum ScriptLanguage {
        JAVASCRIPT;
    }
}
