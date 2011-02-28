package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.springframework.data.graph.core.NodeBacked;

/**
 * @author mh
 * @since 26.02.11
 */
public class EntityEvaluator<S extends NodeBacked, E extends NodeBacked> implements Evaluator {
    private GraphDatabaseContext graphDatabaseContext;

    @Override
    public Evaluation evaluate(Path path) {
        return evaluate(new EntityPath<S,E>(graphDatabaseContext, path));
    }

}
