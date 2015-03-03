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

package org.neo4j.ogm.entityaccess;

import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;

import java.lang.reflect.Field;

public class FieldWriter extends EntityAccess {

    private final FieldInfo fieldInfo;
    private final Field field;
    private final Class<?> fieldType;

    public FieldWriter(ClassInfo classInfo, FieldInfo fieldInfo) {
        this.fieldInfo = fieldInfo;
        this.field = classInfo.getField(fieldInfo);
        this.fieldType = this.field.getType();
    }

    public static void write(Field field, Object instance, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object read(Field field, Object instance) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(Object instance, Object value) {
        if (fieldInfo.hasConverter()) {
            value = fieldInfo.converter().toEntityAttribute(value);
        }
        FieldWriter.write(field, instance, value);
    }

    @Override
    public Class<?> type() {
        if (fieldInfo.hasConverter()) {
            try {
                return fieldInfo.converter().getClass().getDeclaredMethod("toGraphProperty", fieldType).getReturnType();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return fieldType;
    }

    @Override
    public String relationshipName() {
        return this.fieldInfo.relationship();
    }

    @Override
    public String relationshipDirection() {
        return fieldInfo.relationshipDirection();
    }

}
