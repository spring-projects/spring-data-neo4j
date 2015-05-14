/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.repository.query.derived;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.query.GraphQueryMethod;
import org.springframework.data.neo4j.repository.query.GraphRepositoryQuery;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 */
public class DerivedGraphRepositoryQuery extends GraphRepositoryQuery {

    private static final Pattern PREFIX_TEMPLATE = Pattern.compile("^(find|read|get|query)((\\p{Lu}.*?))??By");


    private String query;
    private DerivedQueryDefinition queryDefinition;

    public DerivedGraphRepositoryQuery(GraphQueryMethod graphQueryMethod, RepositoryMetadata metadata, Session session) {
        super(graphQueryMethod, session);
        EntityMetadata<?> info = graphQueryMethod.getEntityInformation();
        PartTree tree = new PartTree(graphQueryMethod.getName(), info.getJavaType());
        this.queryDefinition = new DerivedQueryCreator(tree, info.getJavaType()).createQuery();

        //TODO: should this be eager?
        //TODO who cares now, get rid of it all
       // this.query = buildQuery(method, metadata.getDomainType());
    }

    // FIXME: The hackiest thing i could do to get something working.
    // The easiest way to get this working is to use the existing graph repository dispatching and just override the
    // getQuery() method to return a derived string.
    private String buildQuery(Method method, Class<?> type) {
        String methodName = method.getName();
        Matcher matcher = PREFIX_TEMPLATE.matcher(methodName);
        if (!matcher.find()) {
            throw new RuntimeException("Could not derive query for method: " + methodName + ". Check spelling or use @Query.");
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("MATCH (o:");

        //TODO: Work out how to support RelationshipEntity.
        //TODO We should be using ClassInfo to get this sort of information, to be refactored.
        //moved to cypherfinderquery
        NodeEntity annotation = type.getAnnotation(NodeEntity.class);
        if(annotation!=null && annotation.label()!=null && annotation.label().length()>0) {
            queryBuilder.append(annotation.label());
        }
        else {
            queryBuilder.append(type.getSimpleName());
        }
        queryBuilder.append(") WHERE ");

        // TODO: This will be broken down with another regex.
        //Not needed, we get sufficiently complex part objects to worry about now
        // This will be a for each through each token in the predicates string; i = 0 to match query parameter number.
        String predicates = methodName.substring(matcher.group().length());
        queryBuilder.append("o.");

        // FIXME: Shady way up lowercasing the camel casing to match a propertyName!
        // Spring stuff we extended does the shady stuff for us, drop this
        String property = predicates.substring(0, 1).toLowerCase() + predicates.substring(1);

        //moved to cypherfinderquery

        try {
            Property propertyAnnotation = type.getDeclaredField(property).getAnnotation(Property.class);
            if(propertyAnnotation != null && propertyAnnotation.name()!=null && propertyAnnotation.name().length()>0) {
                property = propertyAnnotation.name();
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Could not find property " + property + " on class " + type.getSimpleName() + ". Check spelling or use @Query.");
        }
        queryBuilder.append(property);

        //TODO: Use a lookup table to match SDC behaviour to actual sign (see Part.java)
        //TODO have the parameters in the cypherfinderquery?? associated with their operators
        queryBuilder.append(" = ");
        queryBuilder.append("{0}");

        queryBuilder.append(" RETURN o");
        return queryBuilder.toString();
    }

    public String getQuery() { //todo get rid of this
        return queryDefinition.toQueryString();
    }

    @Override
    protected String getQueryString() {
        return queryDefinition.toQueryString();
    }

    protected Object execute(Class<?> returnType, Class<?> concreteType, String cypherQuery, Map<String, Object> queryParams) {
        if (returnType.equals(Void.class)) { //TODO this returns statistics now, what do we do with it?
            session.execute(cypherQuery, queryParams);
            return null;
        }

        if (Iterable.class.isAssignableFrom(returnType)) {
            // Special method to handle SDN Iterable<Map<String, Object>> behaviour.
            // TODO: Do we really want this method in an OGM? It's a little too low level and/or doesn't really fit.
            if (Map.class.isAssignableFrom(concreteType)) {
                return session.query(cypherQuery, queryParams);
            }
            return session.query(concreteType, cypherQuery, queryParams);
        }

        return session.queryForObject(returnType, cypherQuery, queryParams);
    }
}
