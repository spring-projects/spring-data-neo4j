package org.springframework.datastore.graph.api;

import org.neo4j.graphdb.traversal.TraversalDescription;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 15.09.2010
 */
public interface FieldTraversalDescriptionBuilder {
    TraversalDescription build(NodeBacked start, Field field);
}
