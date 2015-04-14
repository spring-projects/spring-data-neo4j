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

package org.neo4j.ogm.entityaccess;

import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.MethodInfo;

/**
 * @author Adam George
 * @author Luanne Misquitta
 */
public class MethodReader implements RelationalReader, PropertyReader {

    private final ClassInfo classInfo;
    private final MethodInfo methodInfo;

    MethodReader(ClassInfo classInfo, MethodInfo methodInfo) {
        this.classInfo = classInfo;
        this.methodInfo = methodInfo;
    }

    @Override
    public Object read(Object instance) {
        Object value = MethodWriter.read(classInfo.getMethod(methodInfo), instance);
        if (methodInfo.hasConverter()) {
            value = methodInfo.converter().toGraphProperty(value);
        }
        return value;
    }

    @Override
    public String relationshipType() {
        return methodInfo.relationship();
    }

    @Override
    public String relationshipDirection() {
        try {
            return methodInfo.getAnnotations().get(Relationship.CLASS).get(Relationship.DIRECTION, Relationship.UNDIRECTED);
        } catch (NullPointerException npe) {
            return Relationship.UNDIRECTED;
        }
    }

    @Override
    public String propertyName() {
        return methodInfo.property();
    }

}
