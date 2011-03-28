package org.springframework.data.graph.neo4j.repository;

/**
 * @author mh
 * @since 28.03.11
 */

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.repository.support.AbstractEntityInformation;

public class GraphMetamodelEntityInformation<S extends PropertyContainer, T extends GraphBacked<S>> extends AbstractEntityInformation<T,Long> implements GraphEntityInformation<S,T> {

    private final boolean isNodeEntity;
    private final boolean isPartialEntity;
    private GraphDatabaseContext graphDatabaseContext;

    public GraphMetamodelEntityInformation(Class domainClass, GraphDatabaseContext graphDatabaseContext) {
        super(domainClass);
        this.graphDatabaseContext = graphDatabaseContext;
        NodeEntity nodeEntity = getJavaType().getAnnotation(NodeEntity.class);
        isNodeEntity = nodeEntity!=null;
        isPartialEntity = isNodeEntity && nodeEntity.partial();
    }

    @Override
    public boolean isNodeEntity() {
        return isNodeEntity;
    }

    @Override
    public boolean isPartialEntity() {
        return isPartialEntity;
    }

    @Override
    public boolean isNew(T entity) {
        return entity.hasPersistentState();
    }


    @Override
    public Long getId(T entity) {
        return isNodeEntity() ? ((NodeBacked)entity).getNodeId() : ((RelationshipBacked)entity).getRelationshipId();
    }

    @Override
    public Class<Long> getIdType() {
        return Long.class;
    }
}