package org.springframework.data.graph.neo4j.transaction;

import org.neo4j.graphdb.Path;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.EntityPath;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.template.PathMapper;

/**
 * @author mh
 * @since 26.02.11
 */
public abstract class EntityMapper<S extends NodeBacked, E extends NodeBacked, T> implements PathMapper<T> {
    private GraphDatabaseContext graphDatabaseContext;

    protected EntityMapper(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public abstract T mapPath(EntityPath<S,E> entityPath);

    @Override
    public T mapPath(Path path) {
        return mapPath(new EntityPath<S,E>(graphDatabaseContext, path));
    }

    public abstract static class WithoutResult<S extends NodeBacked,E extends NodeBacked> extends EntityMapper<S,E,Void> {
        protected WithoutResult(GraphDatabaseContext graphDatabaseContext) {
            super(graphDatabaseContext);
        }

        @Override
        public Void mapPath(EntityPath<S, E> entityPath) {
            doWithPath(entityPath);
            return null;
        }

        public abstract void doWithPath(EntityPath<S, E> entityPath);
    }
}
