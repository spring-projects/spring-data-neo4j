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
package org.neo4j.ogm.session.delegates;

import org.neo4j.ogm.cypher.statement.ParameterisedStatement;
import org.neo4j.ogm.entityaccess.FieldWriter;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.session.Capability;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.request.strategy.DeleteNodeStatements;
import org.neo4j.ogm.session.request.strategy.DeleteRelationshipStatements;
import org.neo4j.ogm.session.request.strategy.DeleteStatements;
import org.neo4j.ogm.session.response.Neo4jResponse;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * @author: Vince Bickers
 */
public class DeleteDelegate implements Capability.Delete {

    private final Neo4jSession session;

    public DeleteDelegate(Neo4jSession neo4jSession) {
        this.session = neo4jSession;
    }

    private DeleteStatements getDeleteStatementsBasedOnType(Class type) {
        if (session.metaData().isRelationshipEntity(type.getName())) {
            return new DeleteRelationshipStatements();
        }
        return new DeleteNodeStatements();
    }


    private <T> void deleteAll(T object) {
        List<T> list;
        if (object.getClass().isArray()) {
            list = Arrays.asList(object);
        } else {
            list = (List<T>) object;
        }
        for (T element : list) {
            delete(element);
        }
    }

    @Override
    public <T> void delete(T object) {
        if (object.getClass().isArray() || Iterable.class.isAssignableFrom(object.getClass())) {
            deleteAll(object);
        } else {
            ClassInfo classInfo = session.metaData().classInfo(object);
            if (classInfo != null) {
                Field identityField = classInfo.getField(classInfo.identityField());
                Long identity = (Long) FieldWriter.read(identityField, object);
                if (identity != null) {
                    String url = session.ensureTransaction().url();
                    ParameterisedStatement request = getDeleteStatementsBasedOnType(object.getClass()).delete(identity);
                    try (Neo4jResponse<String> response = session.requestHandler().execute(request, url)) {
                        session.context().clear(object);
                    }
                }
            } else {
                session.info(object.getClass().getName() + " is not an instance of a persistable class");
            }
        }
    }

    @Override
    public <T> void deleteAll(Class<T> type) {
        ClassInfo classInfo = session.metaData().classInfo(type.getName());
        if (classInfo != null) {
            String url = session.ensureTransaction().url();
            ParameterisedStatement request = getDeleteStatementsBasedOnType(type).deleteByType(session.entityType(classInfo.name()));
            try (Neo4jResponse<String> response = session.requestHandler().execute(request, url)) {
                session.context().clear(type);
            }
        } else {
            session.info(type.getName() + " is not a persistable class");
        }
    }


    @Override
    public void purgeDatabase() {
        String url = session.ensureTransaction().url();
        session.requestHandler().execute(new DeleteNodeStatements().purge(), url).close();
        session.context().clear();
    }

    @Override
    public void clear() {
        session.context().clear();
    }
}
