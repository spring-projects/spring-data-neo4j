/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
import org.neo4j.ogm.session.result.RowModel;

import java.lang.reflect.Field;
import java.util.*;

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
            for (NodeModel nodeModel : graphModel.getNodes()) {
                if (nodeModel.getPropertyList().contains(filter)) {
                    objects.add((T) mappingContext.get(nodeModel.getId()));
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
//        if (ref == null) {
//            throw new RuntimeException("Object of type " + type + " with id " + id + " expected in mapping context, but was not found");
//        }
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
