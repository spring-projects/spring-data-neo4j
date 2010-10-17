package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.annotations.RelationshipEndNode;
import org.springframework.datastore.graph.annotations.RelationshipStartNode;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
@Configurable
public class RelationshipNodeFieldAccessorFactory implements FieldAccessorFactory<RelationshipBacked> {
    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Override
    public boolean accept(final Field f) {
        return isStartNodeField(f) || isEndNodeField(f);
    }

    private boolean isEndNodeField(final Field f) {
        return f.isAnnotationPresent(RelationshipEndNode.class);
    }

    private boolean isStartNodeField(final Field f) {
        return f.isAnnotationPresent(RelationshipStartNode.class);
    }

    @Override
    public FieldAccessor<RelationshipBacked, ?> forField(final Field f) {
        if (isStartNodeField(f)) {
            return new RelationshipNodeFieldAccessor(f, graphDatabaseContext) {
                @Override
                protected Node getNode(final Relationship relationship) {
                    return relationship.getStartNode();
                }
            };

        }
        if (isEndNodeField(f)) {
            return new RelationshipNodeFieldAccessor(f, graphDatabaseContext) {
                @Override
                protected Node getNode(final Relationship relationship) {
                    return relationship.getEndNode();
                }
            };
        }
        return null;
    }

    public static abstract class RelationshipNodeFieldAccessor implements FieldAccessor<RelationshipBacked, Object> {

        private final Field field;
        private final GraphDatabaseContext graphDatabaseContext;

        public RelationshipNodeFieldAccessor(final Field field, final GraphDatabaseContext graphDatabaseContext) {
            this.field = field;
            this.graphDatabaseContext = graphDatabaseContext;
        }

        @Override
        public Object setValue(final RelationshipBacked relationshipBacked, final Object newVal) {
            throw new InvalidDataAccessApiUsageException("Cannot change start or end node of existing relationship.");
        }

        @Override
        public Object getValue(final RelationshipBacked relationshipBacked) {
            final Relationship relationship = relationshipBacked.getUnderlyingState();
            final Node node = getNode(relationship);
            if (node == null) {
                return null;
            }
            final NodeBacked result = graphDatabaseContext.createEntityFromState(node, (Class<? extends NodeBacked>) field.getType());
            return doReturn(result);
        }

        protected abstract Node getNode(Relationship relationship);

        @Override
        public boolean isWriteable(final RelationshipBacked relationshipBacked) {
            return false;
        }
    }

}
