package org.neo4j.ogm.session.delegates;

import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.GraphRowModelQuery;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.Query;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.metadata.RelationshipUtils;
import org.neo4j.ogm.metadata.info.AnnotationInfo;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.neo4j.ogm.model.GraphModel;
import org.neo4j.ogm.session.Capability;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.request.strategy.QueryStatements;
import org.neo4j.ogm.session.response.Neo4jResponse;
import org.neo4j.ogm.session.result.GraphRowModel;

import java.util.Collection;

/**
 * @author: Vince Bickers
 */
public class LoadByTypeDelegate implements Capability.LoadByType {

    private final Neo4jSession session;

    public LoadByTypeDelegate(Neo4jSession session) {
        this.session = session;
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, SortOrder sortOrder, Pagination pagination, int depth) {

        String url = session.ensureTransaction().url();
        String entityType = session.entityType(type.getName());
        QueryStatements queryStatements = session.queryStatementsFor(type);

        if (filters.isEmpty()) {

            Query qry = queryStatements.findByType(entityType, depth)
                    .setSortOrder(sortOrder)
                    .setPage(pagination);

            try (Neo4jResponse<GraphModel> response = session.requestHandler().execute(qry, url)) {
                return session.responseHandler().loadAll(type, response);
            }
        } else {

            filters = resolvePropertyAnnotations(type, filters);

            Query qry = queryStatements.findByProperties(entityType, filters, depth)
                    .setSortOrder(sortOrder)
                    .setPage(pagination);

            try (Neo4jResponse<GraphRowModel> response = session.requestHandler().execute((GraphRowModelQuery) qry, url)) {
                return session.responseHandler().loadByProperty(type, response);
            }
        }
    }


    @Override
    public <T> Collection<T> loadAll(Class<T> type) {
        return loadAll(type, new Filters(), new SortOrder(), null, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, int depth) {
        return loadAll(type, new Filters(), new SortOrder(), null, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter) {
        return loadAll(type, new Filters().add(filter), new SortOrder(), null, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, int depth) {
        return loadAll(type, new Filters().add(filter), new SortOrder(), null, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, SortOrder sortOrder) {
        return loadAll(type, new Filters().add(filter), sortOrder, null, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, SortOrder sortOrder, int depth) {
        return loadAll(type, new Filters().add(filter), sortOrder, null, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, Pagination pagination) {
        return loadAll(type, new Filters().add(filter), new SortOrder(), pagination, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, Pagination pagination, int depth) {
        return loadAll(type, new Filters().add(filter), new SortOrder(), pagination, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, SortOrder sortOrder, Pagination pagination) {
        return loadAll(type, new Filters().add(filter), sortOrder, pagination, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filter filter, SortOrder sortOrder, Pagination pagination, int depth) {
        return loadAll(type, new Filters().add(filter), sortOrder, pagination, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters) {
        return loadAll(type, filters, new SortOrder(), null, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, int depth) {
        return loadAll(type, filters, new SortOrder(), null, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, SortOrder sortOrder) {
        return loadAll(type, filters, sortOrder, null, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, SortOrder sortOrder, int depth) {
        return loadAll(type, filters, sortOrder, null, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, Pagination pagination) {
        return loadAll(type, filters, new SortOrder(), pagination, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, Pagination pagination, int depth) {
        return loadAll(type, filters, new SortOrder(), pagination, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Filters filters, SortOrder sortOrder, Pagination pagination) {
        return loadAll(type, filters, sortOrder, pagination, 1);
    }

    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder) {
       return loadAll(type, new Filters(), sortOrder, null, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, int depth) {
        return loadAll(type, new Filters(), sortOrder, null, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Pagination paging) {
        return loadAll(type, new Filters(), new SortOrder(), paging, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Pagination paging, int depth) {
        return loadAll(type, new Filters(), new SortOrder(), paging, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, Pagination pagination) {
        return loadAll(type, new Filters(), sortOrder, pagination, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, Pagination pagination, int depth) {
        return loadAll(type, new Filters(), sortOrder, pagination, depth);
    }

    private Filters resolvePropertyAnnotations(Class entityType, Filters filters) {
        for(Filter filter : filters) {
            if(filter.getOwnerEntityType() == null) {
                filter.setOwnerEntityType(entityType);
            }
            filter.setPropertyName(resolvePropertyName(filter.getOwnerEntityType(), filter.getPropertyName()));
            if(filter.isNested()) {
                resolveRelationshipType(filter);
                ClassInfo nestedClassInfo = session.metaData().classInfo(filter.getNestedPropertyType().getName());
                filter.setNestedEntityTypeLabel(session.entityType(nestedClassInfo.name()));
            }
        }
        return filters;
    }

    private String resolvePropertyName(Class entityType, String propertyName) {
        ClassInfo classInfo = session.metaData().classInfo(entityType.getName());
        FieldInfo fieldInfo = classInfo.propertyFieldByName(propertyName);
        if (fieldInfo != null && fieldInfo.getAnnotations() != null) {
            AnnotationInfo annotation = fieldInfo.getAnnotations().get(Property.CLASS);
            if (annotation != null) {
                return annotation.get(Property.NAME, propertyName);
            }
        }
        return propertyName;
    }

    private void resolveRelationshipType(Filter parameter) {
        ClassInfo classInfo = session.metaData().classInfo(parameter.getOwnerEntityType().getName());
        FieldInfo fieldInfo = classInfo.relationshipFieldByName(parameter.getNestedPropertyName());

        String defaultRelationshipType = RelationshipUtils.inferRelationshipType(parameter.getNestedPropertyName());
        parameter.setRelationshipType(defaultRelationshipType);
        parameter.setRelationshipDirection(Relationship.UNDIRECTED);
        if(fieldInfo.getAnnotations() != null) {
            AnnotationInfo annotation = fieldInfo.getAnnotations().get(Relationship.CLASS);
            if(annotation != null) {
                parameter.setRelationshipType(annotation.get(Relationship.TYPE, defaultRelationshipType));
                parameter.setRelationshipDirection(annotation.get(Relationship.DIRECTION, Relationship.UNDIRECTED));
            }
        }
    }

}
