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

package org.neo4j.ogm.defects.defaultEntityAccessStrategy.relationships;


import org.junit.Test;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.entityaccess.DefaultEntityAccessStrategy;
import org.neo4j.ogm.entityaccess.EntityAccess;
import org.neo4j.ogm.entityaccess.FieldWriter;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.DomainInfo;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RelationshipWriterAnnotatedFieldsTest {

    private DefaultEntityAccessStrategy entityAccessStrategy = new DefaultEntityAccessStrategy();
    private DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.defects.defaultEntityAccessStrategy.relationships");

    @Test
    public void shouldFindWriterForCollection() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "LIST", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the field", objectAccess instanceof FieldWriter);
        assertEquals("LIST", objectAccess.relationshipName());
        assertEquals(List.class, objectAccess.type()) ;

    }

    @Test
    public void shouldFindWriterForScalar() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "SCALAR", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the field", objectAccess instanceof FieldWriter);
        assertEquals("SCALAR", objectAccess.relationshipName());
        assertEquals(T.class, objectAccess.type());

    }


    @Test
    public void shouldFindWriterForArray() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "ARRAY", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the field", objectAccess instanceof FieldWriter);
        assertEquals("ARRAY", objectAccess.relationshipName());
        assertEquals(T[].class, objectAccess.type());

    }

    static class S {

        Long id;

        @Relationship(type="LIST", direction=Relationship.OUTGOING)
        List<T> list;

        @Relationship(type="ARRAY", direction=Relationship.OUTGOING)
        T[] array;

        @Relationship(type="SCALAR", direction=Relationship.OUTGOING)
        T scalar;

    }

    static class T {

        Long id;

    }

    private Class getGenericType(Collection<?> collection) {

        // if we have an object in the collection, use that to determine the type
        if (!collection.isEmpty()) {
            return collection.iterator().next().getClass();
        }

        // otherwise, see if the collection is an anonymous class wrapper
        // new List<T>(){}
        // which does not remove runtime type information

        Class klazz = collection.getClass();

        // obtain anonymous , if any, class for 'this' instance
        final Type superclass = klazz.getGenericSuperclass();

        // obtain Runtime type info of first parameter
        try {
            ParameterizedType parameterizedType = (ParameterizedType) superclass;
            Type[] types = parameterizedType.getActualTypeArguments();
            return (Class) types[0];
        } catch (Exception e) {
            // we can't handle this collection type.
            return null;
        }
    }
}
