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

package org.neo4j.ogm.unit.entityaccess.relationships;


import org.junit.Test;
import org.neo4j.ogm.entityaccess.DefaultEntityAccessStrategy;
import org.neo4j.ogm.entityaccess.EntityAccess;
import org.neo4j.ogm.entityaccess.MethodWriter;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.DomainInfo;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vince Bickers
 */
public class RelationshipWriterPlainSetterTest {

    private DefaultEntityAccessStrategy entityAccessStrategy = new DefaultEntityAccessStrategy();
    private DomainInfo domainInfo = new DomainInfo(this.getClass().getPackage().getName());

    @Test
    public void shouldFindWriterForCollection() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "LIST", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the method", objectAccess instanceof MethodWriter);
        assertEquals("LIST", objectAccess.relationshipName());
        assertEquals(List.class, objectAccess.type()) ;

    }

    @Test
    public void shouldFindWriterForScalar() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "SCALAR", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the method", objectAccess instanceof MethodWriter);
        assertEquals("SCALAR", objectAccess.relationshipName());
        assertEquals(T.class, objectAccess.type());

    }


    @Test
    public void shouldFindWriterForArray() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "ARRAY", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the method", objectAccess instanceof MethodWriter);
        assertEquals("ARRAY", objectAccess.relationshipName());
        assertEquals(T[].class, objectAccess.type());

    }

    static class S {

        Long id;

        List<T> list;
        T[] array;
        T scalar;

        public void setList(List<T> list) {
            this.list = list;
        }

        public void setArray(T[] array) {
            this.array = array;
        }

        public void setScalar(T scalar) {
            this.scalar = scalar;
        }

    }

    static class T {

        Long id;

    }

}
