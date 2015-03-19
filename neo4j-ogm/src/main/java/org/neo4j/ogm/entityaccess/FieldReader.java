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
import org.neo4j.ogm.metadata.info.FieldInfo;

public class FieldReader implements RelationalReader, PropertyReader {

    private final ClassInfo classInfo;
    private final FieldInfo fieldInfo;

    FieldReader(ClassInfo classInfo, FieldInfo fieldInfo) {
        this.classInfo = classInfo;
        this.fieldInfo = fieldInfo;
    }

    @Override
    public Object read(Object instance) {
        Object value = FieldWriter.read(classInfo.getField(fieldInfo), instance);
        if (fieldInfo.hasConverter()) {
            value = fieldInfo.converter().toGraphProperty(value);
        }
        return value;
    }

    @Override
    public String relationshipType() {
        return fieldInfo.relationship();
    }

    @Override
    public String propertyName() {
        return fieldInfo.property();
    }

    @Override
    public String relationshipDirection() {
        try {
            return fieldInfo.getAnnotations().get(Relationship.CLASS).get(Relationship.DIRECTION, Relationship.OUTGOING);
        } catch (NullPointerException npe) {
            return Relationship.OUTGOING;
        }
    }

}
