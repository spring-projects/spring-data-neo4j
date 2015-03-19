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

import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;

import java.lang.reflect.Field;

/**
 * @author Vince Bickers
 */
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

    @Override
    public boolean forScalar() {
        if (Iterable.class.isAssignableFrom(type())) {
            return false;
        }
        if (type().isArray()) {
            return false;
        }
        return true;
    }

}
