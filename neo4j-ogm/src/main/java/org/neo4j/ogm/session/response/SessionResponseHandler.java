/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.session.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.cypher.compiler.CypherContext;
import org.neo4j.ogm.entityaccess.FieldWriter;
import org.neo4j.ogm.mapper.GraphEntityMapper;
import org.neo4j.ogm.mapper.MappedRelationship;
import org.neo4j.ogm.mapper.MappingContext;
import org.neo4j.ogm.mapper.TransientRelationship;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.model.GraphModel;
import org.neo4j.ogm.model.NodeModel;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.model.RelationshipModel;
import org.neo4j.ogm.session.result.RowModel;

import java.lang.reflect.Field;
import java.util.*;

/**
 *  @author Vince Bickers
 */
public class SessionResponseHandler implements ResponseHandler {

    private final MetaData metaData;
    private final MappingContext mappingContext;

    public SessionResponseHandler(MetaData metaData, MappingContext mappingContext) {
        this.metaData = metaData;
        this.mappingContext = mappingContext;
    }

    @Override
    public <T> Set<T> loadByProperty(Class<T> type, Neo4jResponse<GraphModel> response, Property<String, Object> filter) {

        GraphEntityMapper ogm = new GraphEntityMapper(metaData, mappingContext);
        Set<T> objects = new HashSet<>();

        GraphModel graphModel;
        while ((graphModel = response.next()) != null) {

            ogm.map(type, graphModel);

            if (metaData.isRelationshipEntity(type.getName())) {
                for (RelationshipModel relationshipModel : graphModel.getRelationships()) {
                    if (relationshipModel.getPropertyList().contains(filter)
                            && mappingContext.getRelationshipEntity(relationshipModel.getId()).getClass().isAssignableFrom(type)) {
                        objects.add(type.cast(mappingContext.getRelationshipEntity(relationshipModel.getId())));
                    }
                }
            } else {
                for (NodeModel nodeModel : graphModel.getNodes()) {
                    if (nodeModel.getPropertyList().contains(filter) && (mappingContext.get(nodeModel.getId()).getClass().isAssignableFrom(type))) {
                        objects.add(type.cast(mappingContext.get(nodeModel.getId())));
                    }
                }
            }
        }
        response.close();

        return objects;
    }

    @Override
    public void updateObjects(CypherContext context, Neo4jResponse<String> response, ObjectMapper mapper) {

        RowModelResponse rowModelResponse = new RowModelResponse(response, mapper);
        String[] variables = rowModelResponse.columns();
        RowModel rowModel;

        Map<String, Long> directRefMap = new HashMap<>();

        while ((rowModel = rowModelResponse.next()) != null) {
            Object[] results = rowModel.getValues();

            for (int i = 0; i < variables.length; i++) {

                String variable = variables[i];

                // create the mapping between the cypher variable and the newly created domain object's
                // identity, as returned by the database
                Long identity = Long.parseLong(results[i].toString());
                directRefMap.put(variable, identity);

                // find the newly created domain object in the context log
                Object persisted = context.getNewObject(variable);

                if (persisted != null) {  // it will be null if the variable represents a simple relationship.

                    // set the id field of the newly created domain object
                    ClassInfo classInfo = metaData.classInfo(persisted);
                    Field identityField = classInfo.getField(classInfo.identityField());
                    FieldWriter.write(identityField, persisted, identity);

                    // ensure the newly created domain object is added into the mapping context
                    if (classInfo.annotationsInfo().get(RelationshipEntity.CLASS) == null) {
                        mappingContext.registerNodeEntity(persisted, identity);
                    } else {
                        mappingContext.registerRelationshipEntity(persisted, identity);
                    }
                }
            }
        }

        // finally, all new relationships just established in the graph need to be added to the mapping context.
        for (Object object : context.log()) {
            if (object instanceof TransientRelationship) {
                MappedRelationship relationship = (((TransientRelationship) object).convert(directRefMap));
                mappingContext.mappedRelationships().add(relationship);
            }
        }

        rowModelResponse.close();
    }

    @Override
    public <T> T loadById(Class<T> type, Neo4jResponse<GraphModel> response, Long id) {
        GraphEntityMapper ogm = new GraphEntityMapper(metaData, mappingContext);
        GraphModel graphModel;
        while ((graphModel = response.next()) != null) {
            ogm.map(type, graphModel);
        }
        response.close();
        return lookup(type, id);
    }

    private <T> T lookup(Class<T> type, Long id) {
        Object ref;
        ClassInfo typeInfo = metaData.classInfo(type.getName());
        if (typeInfo.annotationsInfo().get(RelationshipEntity.CLASS) == null) {
            ref = mappingContext.get(id);
        } else {
            ref = mappingContext.getRelationshipEntity(id);
        }
        return type.cast(ref);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Neo4jResponse<GraphModel> response) {
        Set<T> objects = new HashSet<>();
        GraphEntityMapper ogm = new GraphEntityMapper(metaData, mappingContext);
        GraphModel graphModel;
        while ((graphModel = response.next()) != null) {
            objects.addAll(ogm.map(type, graphModel));
        }
        response.close();
        return objects;
    }

}
